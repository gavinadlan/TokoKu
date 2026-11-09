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

    public FakePaymentGateway(Environment env) {
        if (env.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("app.payment.provider=fake is not allowed in the prod profile");
        }
        byte[] random = new byte[24];
        new SecureRandom().nextBytes(random);
        this.key = HexFormat.of().formatHex(random);
    }

    @Override
    public PaymentSession createPayment(PaymentRequest request) {
        return new PaymentSession("fake-" + request.providerRef(), "https://fake-payment.invalid/pay/" + request.providerRef());
    }

    @Override
    public boolean verifySignature(Notification n) {
        return MidtransSignature.matches(MidtransSignature.compute(n.orderId(), n.statusCode(), n.grossAmount(), key), n.signatureKey());
    }

    /** Membuat notifikasi bertanda tangan sah, seperti yang akan dikirim provider. */
    public Notification signedNotification(String providerRef, String statusCode, String grossAmount, String transactionStatus) {
        return new Notification(providerRef, statusCode, grossAmount,
                MidtransSignature.compute(providerRef, statusCode, grossAmount, key), transactionStatus, null, "fake-tx-" + providerRef);
    }
}
