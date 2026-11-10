package com.tokoku.notification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.web.util.HtmlUtils;

/**
 * Satu template per jenis: subjek + HTML sederhana (tabel, CSS inline, tanpa gambar) + versi teks.
 * Warna mengikuti desain toko: kertas krem, tinta gelap, hijau tua.
 */
public final class MailTemplates {

    private static final String PAPER = "#F6F1E9", INK = "#1B1613", MUTED = "#5F564D", GREEN = "#2E4B3C";

    private MailTemplates() {}

    private record Copy(String subject, String heading, List<String> lines, String button) {}

    public static EmailMessage render(EmailKind kind, MailData d) {
        Copy c = copy(kind, d);
        List<String> text = new ArrayList<>();
        text.add("Halo " + d.recipientName() + ",");
        text.add("");
        c.lines().forEach(text::add);
        text.add("");
        text.add("Pesanan:");
        d.items().forEach(i -> text.add("- " + i.quantity() + " x " + i.name()));
        text.add("Total: " + rupiah(d.total()));
        text.add("");
        text.add(c.button() + ": " + d.orderUrl());
        text.add("");
        text.add("Salam, " + d.storeName());
        return new EmailMessage(d.to(), c.subject(), html(c, d), String.join("\n", text));
    }

    private static Copy copy(EmailKind kind, MailData d) {
        String no = number(d.orderId());
        return switch (kind) {
            case ORDER_CREATED -> new Copy("Pesanan " + no + " dibuat: selesaikan pembayaran", "Pesanan Anda sudah kami terima",
                    List.of("Pesanan " + no + " menunggu pembayaran. Selesaikan sebelum batas waktu supaya stok tetap untuk Anda.",
                            "Buka halaman pesanan untuk membayar."), "Buka pesanan");
            case PAID -> new Copy("Pembayaran pesanan " + no + " diterima", "Pembayaran diterima",
                    List.of("Terima kasih, pembayaran untuk pesanan " + no + " sudah kami terima. Kami akan segera menyiapkannya."), "Lihat pesanan");
            case PROCESSING -> new Copy("Pesanan " + no + " sedang diproses", "Pesanan sedang diproses",
                    List.of("Pesanan " + no + " sedang kami siapkan."), "Lihat pesanan");
            case READY_FOR_PICKUP -> new Copy("Pesanan " + no + " siap diambil", "Pesanan siap diambil", pickupLines(d, no), "Lihat pesanan");
            case SHIPPED -> new Copy("Pesanan " + no + " dikirim", "Pesanan dalam pengiriman",
                    List.of("Pesanan " + no + " sudah dikirim" + (blank(d.courier()) ? "." : " lewat " + d.courier() + "."),
                            blank(d.trackingNumber()) ? "" : "Nomor resi: " + d.trackingNumber()).stream().filter(s -> !s.isEmpty()).toList(), "Lihat pesanan");
            case COMPLETED -> new Copy("Pesanan " + no + " selesai", "Pesanan selesai",
                    List.of("Pesanan " + no + " sudah selesai. Terima kasih sudah berbelanja, semoga kopinya nikmat."), "Lihat pesanan");
            case CANCELLED -> new Copy("Pesanan " + no + " dibatalkan", "Pesanan dibatalkan",
                    List.of("Pesanan " + no + " dibatalkan, karena pembayaran tidak selesai atau dibatalkan. Stok sudah dikembalikan.",
                            "Anda bisa memesan lagi kapan saja."), "Belanja lagi");
        };
    }

    private static List<String> pickupLines(MailData d, String no) {
        List<String> l = new ArrayList<>();
        l.add("Pesanan " + no + " sudah siap diambil di toko.");
        if (!blank(d.pickupCode())) l.add("Kode ambil: " + d.pickupCode() + " (tunjukkan ke kasir).");
        if (!blank(d.storeAddress())) l.add("Alamat: " + d.storeAddress());
        if (!blank(d.openingHours())) l.add("Jam buka: " + d.openingHours());
        if (!blank(d.pickupInstructions())) l.add(d.pickupInstructions());
        return l;
    }

    private static String html(Copy c, MailData d) {
        StringBuilder b = new StringBuilder();
        b.append("<!doctype html><html lang=\"id\"><body style=\"margin:0;background:").append(PAPER)
                .append(";font-family:Georgia,'Times New Roman',serif;color:").append(INK).append("\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:24px 12px\">")
                .append("<table role=\"presentation\" width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;width:100%\">")
                .append("<tr><td style=\"padding:0 0 16px;font-size:20px;color:").append(GREEN).append("\"><b>").append(e(d.storeName())).append("</b></td></tr>")
                .append("<tr><td style=\"background:#ffffff;border:1px solid #E4DACB;padding:24px\">")
                .append("<h1 style=\"margin:0 0 12px;font-size:22px;font-weight:normal;color:").append(INK).append("\">").append(e(c.heading())).append("</h1>")
                .append("<p style=\"margin:0 0 12px;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.5\">Halo ").append(e(d.recipientName())).append(",</p>");
        c.lines().forEach(s -> b.append("<p style=\"margin:0 0 12px;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.5\">").append(e(s)).append("</p>"));
        b.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:16px 0;font-family:Arial,Helvetica,sans-serif;font-size:14px;border-top:1px solid #E4DACB\">");
        d.items().forEach(i -> b.append("<tr><td style=\"padding:8px 0;border-bottom:1px solid #E4DACB\">").append(e(i.name()))
                .append("</td><td align=\"right\" style=\"padding:8px 0;border-bottom:1px solid #E4DACB;color:").append(MUTED).append("\">").append(i.quantity()).append(" x</td></tr>"));
        b.append("<tr><td style=\"padding:8px 0\"><b>Total</b></td><td align=\"right\" style=\"padding:8px 0\"><b>").append(e(rupiah(d.total()))).append("</b></td></tr></table>")
                .append("<a href=\"").append(e(d.orderUrl())).append("\" style=\"display:inline-block;background:").append(GREEN)
                .append(";color:#ffffff;text-decoration:none;font-family:Arial,Helvetica,sans-serif;font-size:15px;padding:10px 18px\">").append(e(c.button())).append("</a>")
                .append("</td></tr><tr><td style=\"padding:16px 0;font-family:Arial,Helvetica,sans-serif;font-size:12px;color:").append(MUTED)
                .append("\">Email ini dikirim otomatis karena Anda membuat pesanan di ").append(e(d.storeName())).append(".</td></tr></table></td></tr></table></body></html>");
        return b.toString();
    }

    static String number(Long id) { return "No. " + String.format("%04d", id); }

    static String rupiah(BigDecimal v) { return "Rp" + String.format(Locale.GERMANY, "%,.0f", v); }

    private static String e(String s) { return HtmlUtils.htmlEscape(s == null ? "" : s); }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
}
