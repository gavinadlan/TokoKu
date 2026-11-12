package com.tokoku.invoice;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.tokoku.common.Theme;

import com.tokoku.order.FulfillmentType;

/**
 * Templat invoice A4 (XHTML untuk openhtmltopdf, yang hanya mendukung CSS 2.1 dan sebagian CSS 3: tanpa flex/grid,
 * jadi tata letak memakai tabel). Gaya mengikuti docs/DESIGN.md (Putaran 2): garis tipis, Fraunces + Hanken Grotesk,
 * tanda "Lunas" berbingkai ganda yang tetap terbaca saat dicetak hitam-putih.
 */
final class InvoiceHtml {

    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final Theme T = Theme.current();
    private static final String INK = T.get("ink"), MUTED = T.get("ink-muted"), LINE = T.get("invoice-line"), ACCENT = T.get("invoice-accent");
    // Varian "sans": judul pakai Hanken Grotesk (tanpa italic), bukan Fraunces.
    private static final String DISPLAY = T.sansDisplay() ? "Hanken Grotesk" : "Fraunces";
    private static final String STAMP_STYLE = T.sansDisplay() ? "normal" : "italic";

    private InvoiceHtml() {}

    static String render(InvoiceView v, ZoneId zone) {
        DateTimeFormatter date = DateTimeFormatter.ofPattern("d MMMM yyyy", ID).withZone(zone);
        StringBuilder h = new StringBuilder(8192);
        h.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/><title>").append(esc(v.number())).append("</title><style>")
         .append(css()).append("</style></head><body>");

        // Kepala: identitas toko kiri, judul dan tanda Lunas kanan
        h.append("<table class=\"w\"><tr><td class=\"top-l\">")
         .append("<div class=\"store\">").append(esc(v.storeName())).append("</div>")
         .append("<div class=\"muted\">").append(esc(v.storeAddress())).append("</div>")
         .append("<div class=\"muted\">").append(esc(v.storePhone()));
        if (notBlank(v.storeEmail())) h.append(" &#183; ").append(esc(v.storeEmail()));
        h.append("</div>");
        if (notBlank(v.taxId())) h.append("<div class=\"muted\">NPWP/NIB: ").append(esc(v.taxId())).append("</div>");
        h.append("</td><td class=\"top-r\"><div class=\"title\">Invoice</div><div class=\"no\">").append(esc(v.number())).append("</div>")
         .append("<table class=\"paid\" align=\"right\"><tr><td><div class=\"paid-t\">Lunas</div><div class=\"paid-s\">dibayar ")
         .append(esc(date.format(v.paidAt()))).append("</div></td></tr></table></td></tr></table>");
        h.append("<div class=\"rule\"></div>");

        // Meta
        h.append("<table class=\"w meta\"><tr>")
         .append(meta("Nomor order", v.orderNumber())).append(meta("Tanggal terbit", date.format(v.issuedAt())))
         .append(meta("Tanggal bayar", date.format(v.paidAt()))).append(meta("Metode pembayaran", v.paymentMethod()))
         .append("</tr></table>");

        // Pihak: pembeli dan pengiriman/pengambilan
        h.append("<table class=\"w parties\"><tr><td class=\"half\"><div class=\"lbl\">Ditagihkan kepada</div><div class=\"b\">")
         .append(esc(v.buyerName())).append("</div><div class=\"muted\">").append(esc(v.buyerPhone())).append("</div>");
        if (notBlank(v.buyerEmail())) h.append("<div class=\"muted\">").append(esc(v.buyerEmail())).append("</div>");
        h.append("</td><td class=\"half\">");
        if (v.fulfillmentType() == FulfillmentType.PICKUP) {
            h.append("<div class=\"lbl\">Pengambilan di toko</div><div class=\"b\">").append(esc(v.storeName())).append("</div>")
             .append("<div class=\"muted\">").append(esc(v.storeAddress())).append("</div>");
            if (notBlank(v.openingHours())) h.append("<div class=\"muted\">").append(esc(v.openingHours())).append("</div>");
            if (notBlank(v.pickupCode())) h.append("<div class=\"codewrap\">Kode ambil <span class=\"code\">").append(esc(v.pickupCode())).append("</span></div>");
        } else {
            h.append("<div class=\"lbl\">Dikirim ke</div><div class=\"b\">").append(esc(v.recipientName())).append("</div>")
             .append("<div class=\"muted\">").append(esc(v.shippingAddress() == null ? "" : v.shippingAddress()).replace("\n", "<br/>")).append("</div>");
            if (notBlank(v.courier()) || notBlank(v.trackingNumber())) {
                h.append("<div class=\"muted\">Kurir ").append(esc(nz(v.courier()))).append(notBlank(v.trackingNumber()) ? ", resi <span class=\"ink\">" + esc(v.trackingNumber()) + "</span>" : "").append("</div>");
            }
        }
        h.append("</td></tr></table>");

        // Item
        h.append("<table class=\"w items\"><thead><tr><th class=\"c n\">No.</th><th>Deskripsi</th><th class=\"c q\">Jml</th><th class=\"r p\">Harga satuan</th><th class=\"r p\">Jumlah</th></tr></thead><tbody>");
        int i = 1;
        for (InvoiceView.Line l : v.lines()) {
            h.append("<tr><td class=\"c muted\">").append(i++).append("</td><td>").append(esc(l.name())).append("</td><td class=\"c\">")
             .append(l.quantity()).append("</td><td class=\"r\">").append(rp(l.unitPrice())).append("</td><td class=\"r\">").append(rp(l.amount())).append("</td></tr>");
        }
        h.append("</tbody></table>");

        // Ringkasan
        h.append("<table class=\"w\"><tr><td class=\"sp\"></td><td class=\"sum-cell\"><table class=\"w sum\">")
         .append("<tr><td class=\"muted\">Subtotal</td><td class=\"r\">").append(rp(v.subtotal())).append("</td></tr>");
        if (v.fulfillmentType() == FulfillmentType.PICKUP) {
            h.append("<tr><td class=\"muted\">Ambil di toko</td><td class=\"r\">Gratis</td></tr>");
        } else {
            h.append("<tr><td class=\"muted\">Biaya kirim</td><td class=\"r\">").append(rp(v.shippingFee())).append("</td></tr>");
        }
        h.append("<tr class=\"tot\"><td>Total</td><td class=\"r\">").append(rp(v.total())).append("</td></tr></table>");
        if (notBlank(v.taxNote())) h.append("<div class=\"muted small r\">").append(esc(v.taxNote())).append("</div>");
        h.append("</td></tr></table>");

        h.append("<div class=\"status\">Status pembayaran <b>Lunas</b> &#160;&#160; Metode <b>").append(esc(v.paymentMethod())).append("</b></div>");
        if (notBlank(v.footer())) h.append("<div class=\"foot\">").append(esc(v.footer()).replace("\n", "<br/>")).append("</div>");
        h.append("<div class=\"legal\">").append(esc(v.number())).append(" &#183; Order ").append(esc(v.orderNumber()))
         .append(" &#183; ").append(esc(v.storeName())).append("</div>");
        h.append("</body></html>");
        return h.toString();
    }

    private static String meta(String label, String value) {
        return "<td class=\"mc\"><div class=\"lbl\">" + esc(label) + "</div><div class=\"b\">" + esc(value) + "</div></td>";
    }

    private static String css() {
        return "@page { size: A4; margin: 16mm 16mm 20mm 16mm; border: 0.75pt solid " + LINE + "; padding: 8mm 10mm;"
                + " @bottom-center { content: \"Halaman \" counter(page) \" dari \" counter(pages); font-family: 'Hanken Grotesk'; font-size: 7.5pt; color: " + MUTED + "; } }"
                + "body { font-family: 'Hanken Grotesk'; font-weight: 400; font-size: 9pt; line-height: 1.45; color: " + INK + "; margin: 0; }"
                + "table { border-collapse: collapse; } .w { width: 100%; } td, th { vertical-align: top; padding: 0; }"
                + ".muted { color: " + MUTED + "; } .small { font-size: 7.5pt; } .b { font-weight: 600; } .r { text-align: right; } .c { text-align: center; } .ink { color: " + INK + "; }"
                + ".top-l { width: 55%; } .top-r { width: 45%; text-align: right; }"
                + ".store { font-family: '" + DISPLAY + "'; font-weight: 600; font-size: 17pt; line-height: 1.15; }"
                + ".title { font-family: '" + DISPLAY + "'; font-weight: " + (T.sansDisplay() ? 400 : 300) + "; font-size: 30pt; line-height: 1.05; }"
                + ".no { font-weight: 600; font-size: 10pt; margin-top: 3pt; }"
                + ".paid { margin-top: 8pt; border: 2.25pt double " + ACCENT + "; color: " + ACCENT + "; } .paid td { padding: 4pt 12pt 3pt 12pt; text-align: center; }"
                + ".paid-t { font-family: '" + DISPLAY + "'; font-style: " + STAMP_STYLE + "; font-weight: 400; font-size: 14pt; line-height: 1.1; }"
                + ".paid-s { font-size: 7pt; font-weight: 600; }"
                + ".rule { border-top: 1pt solid " + INK + "; margin: 14pt 0 10pt 0; }"
                + ".meta .mc { width: 25%; padding-right: 8pt; } .lbl { font-size: 7.5pt; color: " + MUTED + "; margin-bottom: 1pt; }"
                + ".parties { margin-top: 14pt; } .parties .half { width: 50%; padding-right: 14pt; }"
                + ".code { font-family: '" + DISPLAY + "'; font-weight: 600; letter-spacing: 2pt; font-size: 11pt; border: 0.75pt solid " + INK + "; padding: 1pt 6pt; }"
                + ".codewrap { margin-top: 4pt; font-size: 8pt; color: " + MUTED + "; }"
                + ".items { margin-top: 16pt; } .items th { font-size: 7.5pt; font-weight: 600; color: " + MUTED + "; text-align: left; padding: 0 0 4pt 0; border-bottom: 1pt solid " + INK + "; }"
                + ".items td { padding: 5pt 0; border-bottom: 0.5pt solid " + LINE + "; } .items th.r, .items td.r { text-align: right; } .items th.c, .items td.c { text-align: center; }"
                + ".items .n { width: 8%; } .items .q { width: 8%; } .items .p { width: 20%; }"
                + ".sp { width: 55%; } .sum-cell { width: 45%; padding-top: 10pt; } .sum td { padding: 2pt 0; }"
                + ".sum .tot td { border-top: 1pt solid " + INK + "; padding-top: 6pt; font-weight: 600; font-size: 13pt; }"
                + ".status { margin-top: 12pt; font-size: 8pt; color: " + MUTED + "; } .status b { color: " + INK + "; }"
                + ".foot { margin-top: 18pt; font-size: 8pt; color: " + MUTED + "; max-width: 120mm; }"
                + ".legal { margin-top: 10pt; padding-top: 5pt; border-top: 0.5pt solid " + LINE + "; font-size: 7pt; color: " + MUTED + "; }";
    }

    private static String rp(BigDecimal v) {
        NumberFormat nf = NumberFormat.getIntegerInstance(ID);
        return "Rp " + nf.format(v.setScale(0, java.math.RoundingMode.HALF_UP));
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private static String nz(String s) { return s == null ? "" : s; }

    static String esc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&' -> b.append("&amp;");
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '"' -> b.append("&quot;");
                default -> b.append(c < 0x20 && c != '\n' ? ' ' : c);
            }
        }
        return b.toString();
    }

    static String fmt(Instant i, ZoneId zone) { return DateTimeFormatter.ofPattern("d MMMM yyyy", ID).withZone(zone).format(i); }
}
