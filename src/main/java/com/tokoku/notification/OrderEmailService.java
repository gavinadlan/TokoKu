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
    private final JdbcTemplate jdbc;
    private final TransactionTemplate readTx;
    private final String frontendBaseUrl;
    private final Duration backoff;

    @Autowired
    public OrderEmailService(EmailSender sender, OrderRepository orders, UserRepository users, StoreService store,
            JdbcTemplate jdbc, PlatformTransactionManager tm,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${app.mail.retry-backoff-ms:2000}") long backoffMs) {
        this(sender, orders, users, store, jdbc, tm, frontendBaseUrl, Duration.ofMillis(backoffMs));
    }

    OrderEmailService(EmailSender sender, OrderRepository orders, UserRepository users, StoreService store,
            JdbcTemplate jdbc, PlatformTransactionManager tm, String frontendBaseUrl, Duration backoff) {
        this.sender = sender;
        this.orders = orders;
        this.users = users;
        this.store = store;
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
        Optional<EmailMessage> message = readTx.execute(s -> build(orderId, kind));
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

    private Optional<EmailMessage> build(Long orderId, EmailKind kind) {
        Order o = orders.findById(orderId).orElse(null);
        if (o == null) return Optional.empty();
        User owner = users.findById(o.getUserId()).orElse(null); // penerima = pemilik pesanan, tidak ada sumber lain
        if (owner == null || owner.getEmail() == null || owner.getEmail().isBlank()) return Optional.empty();
        StoreSettings s = store.current();
        MailData d = new MailData(o.getId(), owner.getName(), owner.getEmail(), o.getTotalAmount(),
                o.getFulfillmentType() == FulfillmentType.PICKUP,
                o.getItems().stream().map(i -> new MailData.Line(i.getProductName(), i.getQuantity())).toList(),
                o.getPickupCode(), o.getCourier(), o.getTrackingNumber(),
                frontendBaseUrl + "/order/" + o.getId(), s.getName(), s.getAddress(), s.getOpeningHours(), s.getPickupInstructions());
        return Optional.of(MailTemplates.render(kind, d));
    }

    private static void pause(Duration d) {
        try { Thread.sleep(d.toMillis()); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** Untuk endpoint dev: contoh data tetap, tidak menyentuh DB. */
    public static MailData sample(String to, String baseUrl) {
        return new MailData(1L, "Pengguna Uji", to, new java.math.BigDecimal("100000"), true,
                List.of(new MailData.Line("Kopi Arabika Gayo 250g", 1)), "K7M2QX", "JNE", "CGK123456",
                baseUrl + "/order/1", "TokoKu", "Jl. Contoh No. 1", "Senin-Sabtu 08.00-17.00", "");
    }
}
