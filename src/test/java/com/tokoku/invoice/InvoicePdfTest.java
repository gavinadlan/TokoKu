package com.tokoku.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import com.tokoku.order.FulfillmentType;

/** Render PDF tanpa Spring: isi, memori kecil untuk 10 baris item, dan keterbacaan tanda Lunas saat hitam-putih. */
class InvoicePdfTest {

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");

    private InvoiceView view(FulfillmentType type, int items) {
        List<InvoiceView.Line> lines = new ArrayList<>();
        BigDecimal sub = BigDecimal.ZERO;
        String[] names = {"Kopi Arabika Gayo 250g", "Dripper V60 Keramik 02", "Kertas Filter V60 02 isi 100", "Grinder Manual Burr Keramik",
                "Timbangan Kopi Digital 0.1g", "Teko Leher Angsa 600ml", "Mug Keramik Matte 300ml", "Gelas Cortado 150ml Double Wall",
                "Termometer Teko Digital", "Toples Kedap Udara 500g"};
        for (int i = 0; i < items; i++) {
            BigDecimal price = new BigDecimal(45000 + i * 12500);
            lines.add(new InvoiceView.Line(names[i % names.length], 1 + i % 3, price, price.multiply(BigDecimal.valueOf(1 + i % 3))));
            sub = sub.add(price.multiply(BigDecimal.valueOf(1 + i % 3)));
        }
        BigDecimal fee = type == FulfillmentType.PICKUP ? BigDecimal.ZERO : new BigDecimal("15000");
        Instant t = Instant.parse("2026-11-10T07:32:00Z");
        return new InvoiceView("INV-202611-00042", "0042", t, t, "Transfer bank", "TokoKu", "Jl. Contoh No. 1, Kota Anda", "0800-0000-0000",
                "halo@tokoku.example", null, null, "Senin sampai Sabtu, 09.00 sampai 17.00", "Terima kasih telah berbelanja di TokoKu. Simpan invoice ini sebagai bukti pembayaran.",
                "Siti Rahmawati", "0812 3456 7890", "siti@contoh.example", "Budi Penerima", type,
                type == FulfillmentType.DELIVERY ? "Jl. Kopi No. 12, Menteng\nJakarta Pusat 10310" : null,
                type == FulfillmentType.DELIVERY ? "JNE" : null, type == FulfillmentType.DELIVERY ? "CGK1234567890" : null,
                type == FulfillmentType.PICKUP ? "K7M2PX" : null, lines, sub, fee, sub.add(fee));
    }

    private String text(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void deliveryInvoiceIsAValidPdfWithTheRightContent() throws Exception {
        byte[] pdf = InvoiceService.toPdf(InvoiceHtml.render(view(FulfillmentType.DELIVERY, 3), JAKARTA));
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        String t = text(pdf);
        assertThat(t).contains("INV-202611-00042", "Lunas", "10 November 2026", "Siti Rahmawati", "Budi Penerima", "Dikirim ke", "JNE", "CGK1234567890",
                "Jl. Kopi No. 12", "Biaya kirim", "Kopi Arabika Gayo 250g", "Rp 15.000", "Transfer bank", "Terima kasih telah berbelanja");
        assertThat(t).doesNotContain("Kode ambil").doesNotContain("NPWP");
    }

    @Test
    void pickupInvoiceShowsStoreLocationHoursAndPickupCode() throws Exception {
        String t = text(InvoiceService.toPdf(InvoiceHtml.render(view(FulfillmentType.PICKUP, 3), JAKARTA)));
        assertThat(t).contains("Pengambilan di toko", "Jl. Contoh No. 1, Kota Anda", "Senin sampai Sabtu", "Kode ambil", "Ambil di toko", "Gratis");
        assertThat(t.replaceAll("\\s", "")).contains("K7M2PX");
        assertThat(t).doesNotContain("Biaya kirim").doesNotContain("Dikirim ke");
    }

    @Test
    void taxLinesAppearOnlyWhenConfigured() throws Exception {
        InvoiceView v = view(FulfillmentType.DELIVERY, 2);
        InvoiceView withTax = new InvoiceView(v.number(), v.orderNumber(), v.issuedAt(), v.paidAt(), v.paymentMethod(), v.storeName(), v.storeAddress(),
                v.storePhone(), v.storeEmail(), "12.345.678.9-012.000", "Harga sudah termasuk pajak sesuai ketentuan toko.", v.openingHours(), v.footer(),
                v.buyerName(), v.buyerPhone(), v.buyerEmail(), v.recipientName(), v.fulfillmentType(), v.shippingAddress(), v.courier(), v.trackingNumber(), v.pickupCode(),
                v.lines(), v.subtotal(), v.shippingFee(), v.total());
        String t = text(InvoiceService.toPdf(InvoiceHtml.render(withTax, JAKARTA)));
        assertThat(t).contains("NPWP/NIB: 12.345.678.9-012.000", "Harga sudah termasuk pajak");
    }

    @Test
    void textIsEscapedSoOddBuyerInputCannotBreakTheDocument() throws Exception {
        InvoiceView v = view(FulfillmentType.DELIVERY, 1);
        InvoiceView odd = new InvoiceView(v.number(), v.orderNumber(), v.issuedAt(), v.paidAt(), v.paymentMethod(), v.storeName(), v.storeAddress(),
                v.storePhone(), v.storeEmail(), null, null, v.openingHours(), v.footer(), "Budi <b>&\"Co\"", v.buyerPhone(), v.buyerEmail(), "Penerima <i>",
                v.fulfillmentType(), "Jl. <script>x</script> & Co", v.courier(), v.trackingNumber(), null, v.lines(), v.subtotal(), v.shippingFee(), v.total());
        String t = text(InvoiceService.toPdf(InvoiceHtml.render(odd, JAKARTA)));
        assertThat(t).contains("Budi <b>&\"Co\"").contains("<script>x</script> & Co");
    }

    @Test
    void tenItemInvoiceRendersWithinSmallMemoryAndTime() throws Exception {
        System.gc();
        Runtime rt = Runtime.getRuntime();
        long before = rt.totalMemory() - rt.freeMemory();
        long start = System.nanoTime();
        byte[] pdf = InvoiceService.toPdf(InvoiceHtml.render(view(FulfillmentType.DELIVERY, 10), JAKARTA));
        long ms = (System.nanoTime() - start) / 1_000_000;
        long usedMb = (rt.totalMemory() - rt.freeMemory() - before) / (1024 * 1024);

        assertThat(pdf.length).isBetween(5_000, 400_000);
        assertThat(ms).isLessThan(10_000);
        assertThat(usedMb).as("tambahan heap MB sesudah render (dengan sampah belum dikumpulkan)").isLessThan(128);
        assertThat(text(pdf)).contains("Toples Kedap Udara 500g", "Total");
        System.out.printf("invoice 10 item: %d byte, %d ms, +%d MB heap%n", pdf.length, ms, usedMb);
    }

    @Test
    void paidMarkSurvivesBlackAndWhitePrinting_andSamplesAreWritten() throws Exception {
        byte[] pdf = InvoiceService.toPdf(InvoiceHtml.render(view(FulfillmentType.DELIVERY, 3), JAKARTA));
        File dir = new File("target/invoice-samples");
        dir.mkdirs();
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFRenderer r = new PDFRenderer(doc);
            BufferedImage color = r.renderImageWithDPI(0, 110, ImageType.RGB);
            BufferedImage gray = r.renderImageWithDPI(0, 110, ImageType.GRAY);
            ImageIO.write(color, "png", new File(dir, "invoice-delivery.png"));
            ImageIO.write(gray, "png", new File(dir, "invoice-delivery-gray.png"));
            // Area tanda Lunas (kanan atas): harus ada piksel gelap pekat pada citra skala abu-abu,
            // supaya bingkai dan huruf tidak hilang saat dicetak hitam-putih (ambang 50%).
            int w = gray.getWidth(), h = gray.getHeight();
            int dark = 0;
            for (int y = (int) (h * 0.10); y < h * 0.25; y++) {
                for (int x = (int) (w * 0.60); x < w * 0.95; x++) {
                    if ((gray.getRGB(x, y) & 0xFF) < 110) dark++;
                }
            }
            assertThat(dark).as("piksel gelap di area tanda Lunas").isGreaterThan(300);
        }
        try (PDDocument doc = Loader.loadPDF(InvoiceService.toPdf(InvoiceHtml.render(view(FulfillmentType.PICKUP, 3), JAKARTA)))) {
            ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0, 110, ImageType.RGB), "png", new File(dir, "invoice-pickup.png"));
        }
    }
}
