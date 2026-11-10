package com.tokoku.invoice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tokoku.auth.User;
import com.tokoku.auth.UserRepository;
import com.tokoku.common.ConflictException;
import com.tokoku.common.NotFoundException;
import com.tokoku.order.Order;
import com.tokoku.order.OrderRepository;
import com.tokoku.payment.Payment;
import com.tokoku.payment.PaymentRepository;
import com.tokoku.payment.PaymentStatus;
import com.tokoku.store.StoreService;
import com.tokoku.store.StoreSettings;

@Service
public class InvoiceService {

    private static final Map<String, String> METHODS = Map.ofEntries(
            Map.entry("bank_transfer", "Transfer bank"), Map.entry("echannel", "Mandiri Bill"), Map.entry("qris", "QRIS"),
            Map.entry("gopay", "GoPay"), Map.entry("shopeepay", "ShopeePay"), Map.entry("credit_card", "Kartu kredit"),
            Map.entry("cstore", "Gerai retail"), Map.entry("akulaku", "Akulaku"), Map.entry("fake", "Simulasi pembayaran"));

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final StoreService store;
    private final ZoneId zone;

    public InvoiceService(OrderRepository orders, PaymentRepository payments, UserRepository users, StoreService store,
            @Value("${app.timezone:Asia/Jakarta}") String timezone) {
        this.orders = orders;
        this.payments = payments;
        this.users = users;
        this.store = store;
        this.zone = ZoneId.of(timezone);
    }

    /** Invoice milik pembeli. Order orang lain = 404 (sama seperti detail order). */
    @Transactional(readOnly = true)
    public byte[] forOwner(Long userId, Long orderId) {
        return render(orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> new NotFoundException("Order " + orderId + " not found")));
    }

    @Transactional(readOnly = true)
    public byte[] forAdmin(Long orderId) {
        return render(orders.findById(orderId).orElseThrow(() -> new NotFoundException("Order " + orderId + " not found")));
    }

    @Transactional(readOnly = true)
    public String numberOf(Long orderId) {
        return orders.findById(orderId).map(Order::getInvoiceNumber).orElse(null);
    }

    private byte[] render(Order order) {
        if (order.getInvoiceNumber() == null) {
            throw new ConflictException("Invoice is only available for paid orders");
        }
        StoreSettings s = store.current();
        if (!s.isConfigured()) {
            throw new ConflictException("Store settings are incomplete: the admin must fill in the store name, address and phone before invoices can be issued");
        }
        return toPdf(InvoiceHtml.render(view(order, s), zone));
    }

    private InvoiceView view(Order o, StoreSettings s) {
        Payment payment = payments.findFirstByOrderIdAndStatus(o.getId(), PaymentStatus.PAID).orElse(null);
        User buyer = users.findById(o.getUserId()).orElse(null);
        String method = payment == null || payment.getPaymentType() == null ? "Pembayaran online"
                : METHODS.getOrDefault(payment.getPaymentType(), payment.getPaymentType());
        List<InvoiceView.Line> lines = o.getItems().stream()
                .map(i -> new InvoiceView.Line(i.getProductName(), i.getQuantity(), i.getUnitPrice(), i.getSubtotal())).toList();
        return new InvoiceView(o.getInvoiceNumber(), String.format("%04d", o.getId()), o.getInvoicedAt(),
                payment == null || payment.getPaidAt() == null ? o.getInvoicedAt() : payment.getPaidAt(), method,
                s.getName(), s.getAddress(), s.getPhone(), s.getEmail(), s.getTaxId(), s.getTaxNote(), s.getOpeningHours(), s.getInvoiceFooter(),
                buyer == null ? o.getShippingName() : buyer.getName(), o.getShippingPhone(), buyer == null ? null : buyer.getEmail(), o.getShippingName(),
                o.getFulfillmentType(), o.getShippingAddress(), o.getCourier(), o.getTrackingNumber(), o.getPickupCode(),
                lines, o.getSubtotal(), o.getShippingFee(), o.getTotalAmount());
    }

    static byte[] toPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024)) {
            PdfRendererBuilder b = new PdfRendererBuilder();
            b.useFastMode();
            font(b, "Fraunces-Light.ttf", "Fraunces", 300, FontStyle.NORMAL);
            font(b, "Fraunces-SemiBold.ttf", "Fraunces", 600, FontStyle.NORMAL);
            font(b, "Fraunces-Italic.ttf", "Fraunces", 400, FontStyle.ITALIC);
            font(b, "HankenGrotesk-Regular.ttf", "Hanken Grotesk", 400, FontStyle.NORMAL);
            font(b, "HankenGrotesk-SemiBold.ttf", "Hanken Grotesk", 600, FontStyle.NORMAL);
            b.withHtmlContent(html, null);
            b.toStream(out);
            b.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not render invoice PDF", e);
        } catch (Exception e) {
            throw new IllegalStateException("Could not render invoice PDF", e);
        }
    }

    // Font dibaca dari jar tiap render (kecil, ±180 KB) dan di-subset ke PDF supaya memori tetap rendah
    private static void font(PdfRendererBuilder b, String file, String family, int weight, FontStyle style) {
        b.useFont(() -> {
            InputStream in = InvoiceService.class.getResourceAsStream("/fonts/" + file);
            if (in == null) throw new IllegalStateException("Missing font " + file);
            return in;
        }, family, weight, style, true);
    }
}
