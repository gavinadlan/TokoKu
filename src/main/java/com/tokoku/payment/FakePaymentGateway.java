package com.tokoku.payment;

import java.security.SecureRandom;
import java.util.HexFormat;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

// Hanya untuk test otomatis/CI/dev tanpa kunci. Tidak memanggil jaringan. Kunci tanda tangan acak per start.
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "fake")
public class FakePaymentGateway implements PaymentGateway {

    private final String key;
    private final java.util.Map<String, Notification> statuses = new java.util.concurrent.ConcurrentHashMap<>();

    public FakePaymentGateway(Environment env) {
        if (env.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("app.payment.provider=fake is not allowed in the prod profile");
        }
        org.slf4j.LoggerFactory.getLogger(FakePaymentGateway.class).warn("Payment provider: fake (simulasi, tanpa Midtrans)");
        byte[] random = new byte[24];
        new SecureRandom().nextBytes(random);
        this.key = HexFormat.of().formatHex(random);
    }

    @Override
    public PaymentSession createPayment(PaymentRequest request) {
        return new PaymentSession("fake-" + request.providerRef(), "https://fake-payment.invalid/pay/" + request.providerRef());
    }

    /** Status "di sisi provider" untuk rekonsiliasi; kosong sampai test/dev mengaturnya (= Midtrans 404). */
    @Override
    public java.util.Optional<Notification> fetchStatus(String providerRef) {
        return java.util.Optional.ofNullable(statuses.get(providerRef));
    }

    /** Test/dev: atur status yang akan dilaporkan fetchStatus, seolah pembeli membayar di halaman provider. */
    public void setProviderStatus(String providerRef, String grossAmount, String transactionStatus, String fraudStatus) {
        statuses.put(providerRef, new Notification(providerRef, "200", grossAmount, null, transactionStatus, fraudStatus,
                "fake-tx-" + providerRef, "fake"));
    }

    @Override
    public boolean verifySignature(Notification n) {
        return MidtransSignature.matches(MidtransSignature.compute(n.orderId(), n.statusCode(), n.grossAmount(), key), n.signatureKey());
    }

    /** Membuat notifikasi bertanda tangan sah, seperti yang akan dikirim provider. */
    public Notification signedNotification(String providerRef, String statusCode, String grossAmount, String transactionStatus) {
        return new Notification(providerRef, statusCode, grossAmount,
                MidtransSignature.compute(providerRef, statusCode, grossAmount, key), transactionStatus, null, "fake-tx-" + providerRef, "fake");
    }
}
