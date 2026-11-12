package com.tokoku.notification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.web.util.HtmlUtils;

import com.tokoku.common.Theme;

/**
 * Satu template per jenis: subjek + HTML (tabel + CSS inline, tanpa gambar, font sistem karena web font tidak andal
 * di email) + versi teks. Warna dari theme/tema.json (varian aktif). "Pesanan dibuat" dan
 * "pembayaran diterima" memakai tata letak struk lengkap; jenis lain memakai tata letak ringkas.
 */
public final class MailTemplates {

    private static final Theme T = Theme.current();
    private static final String PAPER = T.get("paper"), SURFACE = T.get("surface"), INK = T.get("ink"), MUTED = T.get("ink-muted"), LINE = T.get("line"),
            GREEN = T.get("accent"), ON_GREEN = T.get("on-accent"), OK_BG = T.get("ok-bg"), OK_INK = T.get("ok-ink"), WARN_BG = T.get("warn-bg"), WARN_INK = T.get("warn-ink");
    private static final String SANS = "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif";
    private static final String SERIF = T.sansDisplay() ? SANS : "Georgia,'Times New Roman',serif";

    private MailTemplates() {}

    private record Copy(String subject, String heading, List<String> lines, String button) {}

    public static EmailMessage render(EmailKind kind, MailData d) {
        Copy c = copy(kind, d);
        boolean receipt = kind == EmailKind.ORDER_CREATED || kind == EmailKind.PAID;
        String inner = receipt ? receiptHtml(kind, c, d) : simpleHtml(c, d);
        String html = shell(c.subject(), c.lines().isEmpty() ? c.heading() : c.lines().get(0), inner, d);
        return new EmailMessage(d.to(), c.subject(), html, receipt ? receiptText(kind, c, d) : simpleText(c, d));
    }

    // ---------- teks tiap jenis ----------

    private static Copy copy(EmailKind kind, MailData d) {
        String no = number(d.orderId());
        return switch (kind) {
            case ORDER_CREATED -> new Copy("Pesanan " + no + " dibuat: selesaikan pembayaran", "Pesanan Anda sudah kami terima",
                    List.of("Pesanan " + no + " menunggu pembayaran. Selesaikan sebelum batas waktu supaya stok tetap untuk Anda.",
                            "Buka halaman pesanan untuk membayar."), "Bayar atau lihat pesanan");
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

    // ---------- struk ----------

    private static String receiptHtml(EmailKind kind, Copy c, MailData d) {
        boolean paid = kind == EmailKind.PAID;
        StringBuilder b = new StringBuilder();
        b.append("<h1 style=\"margin:0 0 8px;font-family:").append(SERIF).append(";font-size:24px;line-height:1.25;font-weight:normal;color:").append(INK).append("\">")
                .append(e(c.heading())).append("</h1>");
        c.lines().forEach(s -> b.append(p(s)));
        // status: teks + warna (bukan warna saja)
        b.append("<p style=\"margin:4px 0 20px\"><span style=\"display:inline-block;padding:4px 10px;font-family:").append(SANS)
                .append(";font-size:13px;font-weight:bold;background:").append(paid ? OK_BG : WARN_BG).append(";color:").append(paid ? OK_INK : WARN_INK).append("\">")
                .append(paid ? "Lunas" : "Menunggu pembayaran").append("</span></p>");

        b.append(table("margin:0 0 20px;border-top:1px solid " + LINE));
        meta(b, "Pesanan", number(d.orderId()));
        meta(b, "Tanggal", d.dateLabel());
        meta(b, "Pembeli", d.recipientName());
        meta(b, "Pengambilan", d.pickup() ? "Ambil di toko" : "Pengiriman");
        if (paid && !blank(d.invoiceNumber())) meta(b, "No. invoice", d.invoiceNumber());
        b.append("</table>");

        // tujuan: alamat toko + jam buka, atau alamat kirim
        b.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 20px;background:").append(SURFACE)
                .append(";border:1px solid ").append(LINE).append("\"><tr><td style=\"padding:12px 14px;font-family:").append(SANS).append(";font-size:14px;line-height:1.5;color:").append(INK).append("\">");
        if (d.pickup()) {
            b.append("<b>Ambil di toko</b><br>").append(e(d.storeName()));
            if (!blank(d.storeAddress())) b.append("<br>").append(nl(d.storeAddress()));
            if (!blank(d.openingHours())) b.append("<br><span style=\"color:").append(MUTED).append("\">Jam buka: ").append(e(d.openingHours())).append("</span>");
            if (paid && !blank(d.pickupCode())) b.append("<br>Kode ambil: <b style=\"letter-spacing:1px\">").append(e(d.pickupCode())).append("</b>");
        } else {
            b.append("<b>Dikirim ke</b><br>").append(e(d.shippingName()));
            if (!blank(d.shippingAddress())) b.append("<br>").append(nl(d.shippingAddress()));
        }
        b.append("</td></tr></table>");

        // barang
        b.append(table("margin:0 0 4px;font-family:" + SANS + ";font-size:14px;color:" + INK));
        b.append("<tr>").append(th("Barang", "left")).append(th("Jml", "right")).append(th("Harga", "right")).append(th("Subtotal", "right")).append("</tr>");
        for (MailData.Line i : d.items()) {
            b.append("<tr>").append(td(e(i.name()), "left", false)).append(td(String.valueOf(i.quantity()), "right", false))
                    .append(td(e(rupiah(i.unitPrice())), "right", false)).append(td(e(rupiah(i.subtotal())), "right", false)).append("</tr>");
        }
        b.append("</table>");

        // total
        b.append(table("margin:0 0 24px;font-family:" + SANS + ";font-size:14px;color:" + INK));
        if (!d.pickup()) total(b, "Subtotal", rupiah(d.subtotal()), false); // ambil di toko: subtotal = total, satu baris cukup
        if (!d.pickup()) total(b, "Biaya kirim", d.shippingFee().signum() == 0 ? "Gratis" : rupiah(d.shippingFee()), false);
        total(b, "Total", rupiah(d.total()), true);
        b.append("</table>");

        if (paid && d.invoiceAttached()) b.append(p("Invoice " + d.invoiceNumber() + " terlampir sebagai PDF."));
        b.append(button(c.button(), d.orderUrl()));
        return b.toString();
    }

    private static String receiptText(EmailKind kind, Copy c, MailData d) {
        boolean paid = kind == EmailKind.PAID;
        List<String> t = new ArrayList<>();
        t.add(d.storeName());
        t.add("");
        t.add(c.heading().toUpperCase(Locale.ROOT));
        t.addAll(c.lines());
        t.add("Status: " + (paid ? "Lunas" : "Menunggu pembayaran"));
        t.add("");
        t.add("Pesanan     : " + number(d.orderId()));
        t.add("Tanggal     : " + d.dateLabel());
        t.add("Pembeli     : " + d.recipientName());
        t.add("Pengambilan : " + (d.pickup() ? "Ambil di toko" : "Pengiriman"));
        if (paid && !blank(d.invoiceNumber())) t.add("No. invoice : " + d.invoiceNumber());
        t.add("");
        if (d.pickup()) {
            t.add("Ambil di toko: " + d.storeName() + (blank(d.storeAddress()) ? "" : ", " + d.storeAddress()));
            if (!blank(d.openingHours())) t.add("Jam buka: " + d.openingHours());
            if (paid && !blank(d.pickupCode())) t.add("Kode ambil: " + d.pickupCode());
        } else {
            t.add("Dikirim ke: " + d.shippingName() + (blank(d.shippingAddress()) ? "" : ", " + d.shippingAddress()));
        }
        t.add("");
        t.add("Barang:");
        d.items().forEach(i -> t.add("- " + i.name() + ": " + i.quantity() + " x " + rupiah(i.unitPrice()) + " = " + rupiah(i.subtotal())));
        t.add("");
        if (!d.pickup()) t.add("Subtotal: " + rupiah(d.subtotal()));
        if (!d.pickup()) t.add("Biaya kirim: " + (d.shippingFee().signum() == 0 ? "Gratis" : rupiah(d.shippingFee())));
        t.add("TOTAL: " + rupiah(d.total()));
        if (paid && d.invoiceAttached()) { t.add(""); t.add("Invoice " + d.invoiceNumber() + " terlampir sebagai PDF."); }
        t.add("");
        t.add(c.button() + ": " + d.orderUrl());
        t.add("");
        t.add(contactText(d));
        return String.join("\n", t);
    }

    // ---------- tata letak ringkas ----------

    private static String simpleHtml(Copy c, MailData d) {
        StringBuilder b = new StringBuilder();
        b.append("<h1 style=\"margin:0 0 12px;font-family:").append(SERIF).append(";font-size:24px;line-height:1.25;font-weight:normal;color:").append(INK).append("\">")
                .append(e(c.heading())).append("</h1>").append(p("Halo " + d.recipientName() + ","));
        c.lines().forEach(s -> b.append(p(s)));
        b.append(table("margin:16px 0 4px;font-family:" + SANS + ";font-size:14px;color:" + INK + ";border-top:1px solid " + LINE));
        d.items().forEach(i -> b.append("<tr><td style=\"padding:8px 0;border-bottom:1px solid ").append(LINE).append("\">").append(e(i.name()))
                .append("</td><td align=\"right\" style=\"padding:8px 0;border-bottom:1px solid ").append(LINE).append(";color:").append(MUTED).append("\">").append(i.quantity()).append(" x</td></tr>"));
        b.append("<tr><td style=\"padding:10px 0\"><b>Total</b></td><td align=\"right\" style=\"padding:10px 0\"><b>").append(e(rupiah(d.total()))).append("</b></td></tr></table>");
        b.append("<div style=\"height:20px;line-height:20px\">&nbsp;</div>").append(button(c.button(), d.orderUrl()));
        return b.toString();
    }

    private static String simpleText(Copy c, MailData d) {
        List<String> text = new ArrayList<>();
        text.add("Halo " + d.recipientName() + ",");
        text.add("");
        text.addAll(c.lines());
        text.add("");
        text.add("Pesanan:");
        d.items().forEach(i -> text.add("- " + i.quantity() + " x " + i.name()));
        text.add("Total: " + rupiah(d.total()));
        text.add("");
        text.add(c.button() + ": " + d.orderUrl());
        text.add("");
        text.add(contactText(d));
        return String.join("\n", text);
    }

    // ---------- kerangka ----------

    private static String shell(String title, String preheader, String inner, MailData d) {
        StringBuilder b = new StringBuilder();
        b.append("<!doctype html><html lang=\"id\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<meta name=\"x-apple-disable-message-reformatting\"><title>").append(e(title)).append("</title>")
                // Hanya penyempurnaan ponsel; tampilan dasar sudah lengkap lewat gaya inline
                .append("<style>@media (max-width:480px){.wrap{padding:12px 8px!important}.card{padding:18px 14px!important}.nowrap-sm{white-space:normal!important}}</style></head>")
                .append("<body style=\"margin:0;padding:0;background:").append(PAPER).append(";color:").append(INK).append("\">")
                .append("<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;color:").append(PAPER).append("\">").append(e(preheader)).append("</div>")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:").append(PAPER).append("\"><tr><td align=\"center\" class=\"wrap\" style=\"padding:24px 12px\">")
                .append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:600px\">")
                // kepala toko: nama (wordmark teks, tanpa gambar), alamat dan telepon
                .append("<tr><td style=\"padding:0 0 16px\"><div style=\"font-family:").append(SERIF).append(";font-size:24px;line-height:1.2;color:").append(GREEN).append("\"><b>").append(e(d.storeName())).append("</b></div>");
        String sub = joinNonBlank(" · ", d.storeAddress(), d.storePhone());
        if (!sub.isEmpty()) b.append("<div style=\"margin-top:4px;font-family:").append(SANS).append(";font-size:13px;line-height:1.4;color:").append(MUTED).append("\">").append(e(sub)).append("</div>");
        b.append("</td></tr><tr><td class=\"card\" style=\"background:").append(T.get("card")).append(";border:1px solid ").append(LINE).append(";border-top:4px solid ").append(GREEN).append(";padding:28px\">")
                .append(inner)
                .append("</td></tr><tr><td style=\"padding:16px 0;font-family:").append(SANS).append(";font-size:12px;line-height:1.5;color:").append(MUTED).append("\">")
                .append(e(joinNonBlank(" \u00B7 ", d.storePhone(), d.storeEmail())))
                .append("<br>Email ini dikirim otomatis karena Anda membuat pesanan di ").append(e(d.storeName())).append(".</td></tr></table></td></tr></table></body></html>");
        return b.toString();
    }

    private static String contactText(MailData d) {
        return joinNonBlank("\n", d.storeName(), d.storeAddress(), joinNonBlank(" · ", d.storePhone(), d.storeEmail()));
    }

    private static String table(String style) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"" + style + "\">";
    }

    private static void meta(StringBuilder b, String label, String value) {
        b.append("<tr><td style=\"padding:7px 12px 7px 0;border-bottom:1px solid ").append(LINE).append(";font-family:").append(SANS)
                .append(";font-size:13px;color:").append(MUTED).append(";width:110px;vertical-align:top\">").append(e(label))
                .append("</td><td style=\"padding:7px 0;border-bottom:1px solid ").append(LINE).append(";font-family:").append(SANS)
                .append(";font-size:14px;color:").append(INK).append("\">").append(e(value)).append("</td></tr>");
    }

    private static String th(String text, String align) {
        return "<th align=\"" + align + "\" style=\"padding:8px 6px;border-bottom:2px solid " + INK + ";font-size:12px;font-weight:bold;color:" + MUTED
                + ";text-align:" + align + "\">" + e(text) + "</th>";
    }

    private static String td(String html, String align, boolean bold) {
        return "<td align=\"" + align + "\" style=\"padding:9px 6px;border-bottom:1px solid " + LINE + ";vertical-align:top;text-align:" + align
                + (bold ? ";font-weight:bold" : "") + "\">" + html + "</td>";
    }

    private static void total(StringBuilder b, String label, String value, boolean strong) {
        String s = strong ? "padding:10px 6px;border-top:2px solid " + INK + ";font-size:16px;font-weight:bold" : "padding:4px 6px;color:" + MUTED;
        b.append("<tr><td align=\"right\" style=\"").append(s).append("\">").append(e(label)).append("</td><td align=\"right\" style=\"")
                .append(s).append(";width:120px\">").append(e(value)).append("</td></tr>");
    }

    /** Tombol "antipeluru": sel tabel berlatar hijau, jadi tampil juga di Outlook yang mengabaikan padding tautan. */
    private static String button(String label, String url) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\"><tr><td style=\"background:" + GREEN + "\"><a href=\"" + e(url)
                + "\" style=\"display:inline-block;padding:12px 22px;font-family:" + SANS + ";font-size:15px;font-weight:bold;color:" + ON_GREEN
                + ";text-decoration:none\">" + e(label) + "</a></td></tr></table>";
    }

    private static String p(String s) {
        return "<p style=\"margin:0 0 12px;font-family:" + SANS + ";font-size:15px;line-height:1.55;color:" + INK + "\">" + e(s) + "</p>";
    }

    /** "10 November 2026, 14.07 WIB" (zona toko, bukan zona server). */
    public static String dateLabel(java.time.Instant at, java.time.ZoneId zone) {
        String abbr = zone.getId().equals("Asia/Jakarta") ? "WIB" : zone.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ROOT);
        return java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy, HH.mm", Locale.of("id", "ID")).withZone(zone).format(at) + " " + abbr;
    }

    static String number(Long id) { return "No. " + String.format("%04d", id); }

    /** Sama dengan frontend: "Rp12.500", titik ribuan. */
    static String rupiah(BigDecimal v) { return "Rp" + String.format(Locale.GERMANY, "%,.0f", v); }

    private static String e(String s) { return HtmlUtils.htmlEscape(s == null ? "" : s); }

    private static String nl(String s) { return e(s).replace("\n", "<br>"); }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private static String joinNonBlank(String sep, String... parts) {
        List<String> l = new ArrayList<>();
        for (String s : parts) if (!blank(s)) l.add(s.strip());
        return String.join(sep, l);
    }
}
