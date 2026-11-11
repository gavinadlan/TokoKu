package com.tokoku.notification;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.tokoku.auth.User;
import com.tokoku.auth.UserRepository;
import com.tokoku.invoice.InvoiceService;
import com.tokoku.order.FulfillmentType;
import com.tokoku.order.Order;
import com.tokoku.order.OrderRepository;
import com.tokoku.store.StoreService;
import com.tokoku.store.StoreSettings;

/**
 * Mengirim email pesanan SETELAH transaksi perubahan status commit, di thread pool terpisah. Kegagalan hanya di-log:
 * tidak pernah menggagalkan webhook atau perubahan status. email_log (unique order+jenis) mencegah email dobel
 * saat webhook dikirim ulang; penerima selalu pemilik pesanan.
 */
@Service
public class OrderEmailService {

    private static final Logger log = LoggerFactory.getLogger(OrderEmailService.class);
    static final int MAX_ATTEMPTS = 3;

    private final EmailSender sender;
    private final OrderRepository orders;
    private final UserRepository users;
    private final StoreService store;
    private final InvoiceService invoices;
    private final java.time.ZoneId zone;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate readTx;
    private final String frontendBaseUrl;
    private final Duration backoff;

    @Autowired
    public OrderEmailService(EmailSender sender, OrderRepository orders, UserRepository users, StoreService store,
            InvoiceService invoices, JdbcTemplate jdbc, PlatformTransactionManager tm,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${app.timezone:Asia/Jakarta}") String timezone,
            @Value("${app.mail.retry-backoff-ms:2000}") long backoffMs) {
        this(sender, orders, users, store, invoices, jdbc, tm, frontendBaseUrl, java.time.ZoneId.of(timezone), Duration.ofMillis(backoffMs));
    }

    OrderEmailService(EmailSender sender, OrderRepository orders, UserRepository users, StoreService store,
            InvoiceService invoices, JdbcTemplate jdbc, PlatformTransactionManager tm, String frontendBaseUrl,
            java.time.ZoneId zone, Duration backoff) {
        this.sender = sender;
        this.orders = orders;
        this.users = users;
        this.store = store;
        this.invoices = invoices;
        this.zone = zone;
        this.jdbc = jdbc;
        this.readTx = new TransactionTemplate(tm);
        this.readTx.setReadOnly(true);
        this.frontendBaseUrl = frontendBaseUrl.strip().replaceAll("/+$", "");
        this.backoff = backoff;
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(OrderNotification event) {
        try {
            handle(event.orderId(), event.kind());
        } catch (RuntimeException e) {
            log.warn("Email {} untuk order {} gagal diproses: {}", event.kind(), event.orderId(), e.toString());
        }
    }

    /** Dipisah dari listener agar bisa diuji langsung. Mengembalikan true bila email terkirim. */
    boolean handle(Long orderId, EmailKind kind) {
        if (!sender.delivers()) {
            log.info("Email {} untuk order {} dilewati (pengirim no-op)", kind, orderId);
            return false;
        }
        // Klaim atomik: hanya satu pemanggil yang lolos per (order, jenis); sisanya (webhook ulang) berhenti di sini
        int claimed = jdbc.update("INSERT INTO email_log (order_id, kind, status) VALUES (?, ?, 'SENDING') ON CONFLICT (order_id, kind) DO NOTHING",
                orderId, kind.name());
        if (claimed == 0) {
            log.info("Email {} untuk order {} sudah pernah diproses, dilewati", kind, orderId);
            return false;
        }
        // PDF dibuat DI LUAR transaksi baca: exception dari InvoiceService (mis. 409 identitas toko kosong) akan menandai
        // transaksi yang ikut serta sebagai rollback-only dan menggagalkan seluruh pembuatan email.
        byte[] pdf = kind == EmailKind.PAID ? invoicePdf(orderId) : null;
        Optional<EmailMessage> message = readTx.execute(s -> build(orderId, kind, pdf));
        if (message == null || message.isEmpty()) {
            jdbc.update("UPDATE email_log SET status = 'FAILED' WHERE order_id = ? AND kind = ?", orderId, kind.name());
            log.warn("Email {} untuk order {}: data order/penerima tidak ditemukan", kind, orderId);
            return false;
        }
        EmailMessage m = message.get();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                sender.send(m);
                jdbc.update("UPDATE email_log SET status = 'SENT', attempts = ?, sent_at = now() WHERE order_id = ? AND kind = ?",
                        attempt, orderId, kind.name());
                log.info("Email {} untuk order {} terkirim ke {}", kind, orderId, EmailSender.mask(m.to()));
                return true;
            } catch (RuntimeException e) {
                // Hanya jenis exception yang dicatat: pesan SMTP bisa memuat nama pengguna/alamat
                log.warn("Email {} untuk order {} gagal (percobaan {}/{}): {}", kind, orderId, attempt, MAX_ATTEMPTS, e.getClass().getSimpleName());
                if (attempt < MAX_ATTEMPTS) pause(backoff.multipliedBy(attempt * (long) attempt));
            }
        }
        jdbc.update("UPDATE email_log SET status = 'FAILED', attempts = ? WHERE order_id = ? AND kind = ?", MAX_ATTEMPTS, orderId, kind.name());
        log.warn("Email {} untuk order {} dibatalkan setelah {} percobaan", kind, orderId, MAX_ATTEMPTS);
        return false;
    }

    private Optional<EmailMessage> build(Long orderId, EmailKind kind, byte[] pdf) {
        Order o = orders.findById(orderId).orElse(null);
        if (o == null) return Optional.empty();
        User owner = users.findById(o.getUserId()).orElse(null); // penerima = pemilik pesanan, tidak ada sumber lain
        if (owner == null || owner.getEmail() == null || owner.getEmail().isBlank()) return Optional.empty();
        StoreSettings s = store.current();
        java.time.Instant when = kind == EmailKind.PAID && o.getInvoicedAt() != null ? o.getInvoicedAt() : o.getCreatedAt();
        MailData d = new MailData(o.getId(), owner.getName(), owner.getEmail(), MailTemplates.dateLabel(when, zone),
                o.getFulfillmentType() == FulfillmentType.PICKUP,
                o.getItems().stream().map(i -> new MailData.Line(i.getProductName(), i.getQuantity(), i.getUnitPrice(), i.getSubtotal())).toList(),
                o.getSubtotal(), o.getShippingFee(), o.getTotalAmount(), o.getShippingName(), o.getShippingAddress(),
                o.getPickupCode(), o.getCourier(), o.getTrackingNumber(), o.getInvoiceNumber(), pdf != null && o.getInvoiceNumber() != null,
                frontendBaseUrl + "/order/" + o.getId(), s.getName(), s.getAddress(), s.getPhone(), s.getEmail(),
                s.getOpeningHours(), s.getPickupInstructions());
        EmailMessage m = MailTemplates.render(kind, d);
        if (pdf != null && o.getInvoiceNumber() != null) {
            m = m.withAttachments(List.of(new EmailMessage.Attachment(o.getInvoiceNumber() + ".pdf", "application/pdf", pdf)));
        }
        return Optional.of(m);
    }

    /** Lampiran invoice hanya bila sudah terbit dan bisa dibuat (identitas toko lengkap); kegagalan apa pun = tanpa lampiran. */
    private byte[] invoicePdf(Long orderId) {
        try {
            return invoices.forAdmin(orderId);
        } catch (RuntimeException e) {
            log.info("Invoice order {} tidak dilampirkan: {}", orderId, e.getClass().getSimpleName());
            return null;
        }
    }

    private static void pause(Duration d) {
        try { Thread.sleep(d.toMillis()); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
