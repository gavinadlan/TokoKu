package com.tokoku.notification;

import java.math.BigDecimal;
import java.util.List;

/** Data contoh tetap untuk pratinjau dan email uji di dev: tidak menyentuh DB dan tidak memuat data nyata. */
public final class MailSamples {

    private MailSamples() {}

    public static MailData sample(String to, String baseUrl, boolean pickup, boolean paid) {
        List<MailData.Line> items = List.of(
                new MailData.Line("Kopi Arabika Gayo 250g", 2, new BigDecimal("85000"), new BigDecimal("170000")),
                new MailData.Line("Dripper keramik V60 02", 1, new BigDecimal("125500"), new BigDecimal("125500")),
                new MailData.Line("Filter kertas V60 isi 100", 3, new BigDecimal("12500"), new BigDecimal("37500")));
        BigDecimal subtotal = new BigDecimal("333000");
        BigDecimal fee = pickup ? BigDecimal.ZERO : new BigDecimal("15000");
        return new MailData(128L, "Siti Rahmawati", to, "10 November 2026, 14.07 WIB", pickup, items, subtotal, fee, subtotal.add(fee),
                "Siti Rahmawati", pickup ? null : "Jl. Kopi No. 12, Menteng\nJakarta Pusat 10310", paid && pickup ? "K7M2QX" : null, null, null,
                paid ? "INV-202611-00042" : null, paid, baseUrl + "/order/128",
                "TokoKu", "Jl. Contoh No. 1, Kota Anda", "0800-0000-0000", "halo@tokoku.example", "Senin sampai Sabtu, 08.00 sampai 17.00", "");
    }

    public static MailData sample(String to, String baseUrl) { return sample(to, baseUrl, true, true); }
}
