package com.tokoku.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MailTemplatesTest {

    private static final String BASE = "https://tokoku.example.com";

    private MailData sample(boolean pickup, boolean paid) { return MailSamples.sample("budi@example.com", BASE, pickup, paid); }

    private MailData withNames(String buyer, String item, String address) {
        MailData s = sample(false, true);
        return new MailData(s.orderId(), buyer, s.to(), s.dateLabel(), false,
                List.of(new MailData.Line(item, 1, new BigDecimal("1000"), new BigDecimal("1000"))), s.subtotal(), s.shippingFee(), s.total(),
                buyer, address, null, null, null, s.invoiceNumber(), true, s.orderUrl(), "<i>Toko</i>", s.storeAddress(), s.storePhone(),
                s.storeEmail(), s.openingHours(), s.pickupInstructions());
    }

    @ParameterizedTest
    @EnumSource(EmailKind.class)
    void everyKindHasSubjectWithOrderNumberHtmlTextAndLink(EmailKind kind) {
        EmailMessage m = MailTemplates.render(kind, sample(true, true));
        assertThat(m.to()).isEqualTo("budi@example.com");
        assertThat(m.subject()).contains("0128").doesNotContain("\n");
        assertThat(m.html()).contains(BASE + "/order/128").contains("lang=\"id\"").contains("max-width:600px").contains("viewport");
        assertThat(m.text()).contains(BASE + "/order/128").contains("TokoKu");
        assertThat(m.html()).doesNotContain("<img").doesNotContain("src=").doesNotContain("@import").doesNotContain("<script");
    }

    @Test
    void createdReceiptShowsTotalsDateStatusAndDeliveryDetails() {
        EmailMessage m = MailTemplates.render(EmailKind.ORDER_CREATED, sample(false, false));
        assertThat(m.html()).contains("Menunggu pembayaran").contains("10 November 2026, 14.07 WIB")
                .contains("Kopi Arabika Gayo 250g").contains("Rp85.000").contains("Rp170.000").contains("Rp12.500")
                .contains("Biaya kirim").contains("Rp15.000").contains("Rp348.000")
                .contains("Dikirim ke").contains("Jl. Kopi No. 12, Menteng<br>Jakarta Pusat 10310")
                .doesNotContain("Jam buka: ").doesNotContain("Lunas").doesNotContain("terlampir");
        assertThat(m.text()).contains("TOTAL: Rp348.000").contains("Pengambilan : Pengiriman").contains("Subtotal: Rp333.000").contains("Status: Menunggu pembayaran");
    }

    @Test
    void pickupReceiptShowsStoreAddressHoursAndNoShippingLine() {
        EmailMessage m = MailTemplates.render(EmailKind.ORDER_CREATED, sample(true, false));
        assertThat(m.html()).contains("Ambil di toko").contains("Jl. Contoh No. 1, Kota Anda").contains("Jam buka: Senin sampai Sabtu")
                .contains("Rp333.000").doesNotContain("Biaya kirim").doesNotContain("Dikirim ke");
    }

    @Test
    void freeShippingIsLabelledGratis() {
        MailData s = sample(false, false);
        MailData free = new MailData(s.orderId(), s.recipientName(), s.to(), s.dateLabel(), false, s.items(), s.subtotal(), BigDecimal.ZERO, s.subtotal(),
                s.shippingName(), s.shippingAddress(), null, null, null, null, false, s.orderUrl(), s.storeName(), s.storeAddress(), s.storePhone(),
                s.storeEmail(), s.openingHours(), s.pickupInstructions());
        assertThat(MailTemplates.render(EmailKind.ORDER_CREATED, free).html()).contains("Gratis");
    }

    @Test
    void paidReceiptShowsLunasInvoiceNumberAndAttachmentNoteOnlyWhenAttached() {
        MailData attached = sample(true, true);
        EmailMessage m = MailTemplates.render(EmailKind.PAID, attached);
        assertThat(m.html()).contains("Lunas").contains("INV-202611-00042").contains("terlampir sebagai PDF").contains("K7M2QX");
        assertThat(m.text()).contains("Invoice INV-202611-00042 terlampir");

        MailData noPdf = new MailData(attached.orderId(), attached.recipientName(), attached.to(), attached.dateLabel(), true, attached.items(),
                attached.subtotal(), attached.shippingFee(), attached.total(), attached.shippingName(), null, attached.pickupCode(), null, null,
                "INV-202611-00042", false, attached.orderUrl(), attached.storeName(), attached.storeAddress(), attached.storePhone(),
                attached.storeEmail(), attached.openingHours(), attached.pickupInstructions());
        assertThat(MailTemplates.render(EmailKind.PAID, noPdf).html()).doesNotContain("terlampir");
    }

    @Test
    void readyForPickupCarriesAddressHoursAndCode() {
        EmailMessage m = MailTemplates.render(EmailKind.READY_FOR_PICKUP, sample(true, true));
        assertThat(m.text()).contains("Jl. Contoh No. 1, Kota Anda").contains("Senin sampai Sabtu").contains("K7M2QX");
    }

    @Test
    void userControlledValuesAreHtmlEscapedInEveryPlace() {
        MailData d = withNames("<script>alert(1)</script>", "Kopi <b>\"x\"</b>", "Jl. <img src=x onerror=1>");
        for (EmailKind kind : new EmailKind[] { EmailKind.ORDER_CREATED, EmailKind.PAID, EmailKind.PROCESSING }) {
            EmailMessage m = MailTemplates.render(kind, d);
            assertThat(m.html()).doesNotContain("<script>").doesNotContain("Kopi <b>").doesNotContain("<img").doesNotContain("<i>Toko</i>")
                    .contains("&lt;script&gt;");
            assertThat(m.subject()).doesNotContain("script");
        }
    }

    @Test
    void dateLabelUsesStoreTimeZoneNotServerZone() {
        assertThat(MailTemplates.dateLabel(Instant.parse("2026-11-10T07:07:00Z"), ZoneId.of("Asia/Jakarta"))).isEqualTo("10 November 2026, 14.07 WIB");
        assertThat(MailTemplates.dateLabel(Instant.parse("2026-11-10T20:30:00Z"), ZoneId.of("Asia/Jakarta"))).isEqualTo("11 November 2026, 03.30 WIB");
    }

    @Test
    void rupiahMatchesFrontendThousandsSeparator() {
        assertThat(MailTemplates.rupiah(new BigDecimal("12500.00"))).isEqualTo("Rp12.500");
        assertThat(MailTemplates.rupiah(new BigDecimal("1234567"))).isEqualTo("Rp1.234.567");
    }

    @Test
    void maskHidesMostOfTheAddress() {
        assertThat(EmailSender.mask("budi@example.com")).isEqualTo("bu***@example.com").doesNotContain("budi");
    }
}
