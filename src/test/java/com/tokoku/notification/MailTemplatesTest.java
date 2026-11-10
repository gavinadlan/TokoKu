package com.tokoku.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MailTemplatesTest {

    private MailData data(String name, String item) {
        return new MailData(42L, name, "budi@example.com", new BigDecimal("85000"), true,
                List.of(new MailData.Line(item, 2)), "K7M2QX", "JNE", "CGK123", "https://tokoku.example.com/order/42",
                "TokoKu", "Jl. Kopi No. 1, Jakarta", "Senin-Sabtu 08.00-17.00", "Bawa KTP");
    }

    @ParameterizedTest
    @EnumSource(EmailKind.class)
    void everyKindHasSubjectWithOrderNumberHtmlTextAndLink(EmailKind kind) {
        EmailMessage m = MailTemplates.render(kind, data("Budi", "Kopi"));
        assertThat(m.to()).isEqualTo("budi@example.com");
        assertThat(m.subject()).contains("0042").doesNotContain("\n");
        assertThat(m.html()).contains("https://tokoku.example.com/order/42").contains("Rp85.000").contains("lang=\"id\"");
        assertThat(m.text()).contains("Halo Budi").contains("https://tokoku.example.com/order/42");
        assertThat(m.html()).doesNotContain("<img").doesNotContain("http://").doesNotContain("src=");
    }

    @Test
    void readyForPickupCarriesAddressHoursAndCode() {
        EmailMessage m = MailTemplates.render(EmailKind.READY_FOR_PICKUP, data("Budi", "Kopi"));
        assertThat(m.text()).contains("Jl. Kopi No. 1, Jakarta").contains("Senin-Sabtu 08.00-17.00").contains("K7M2QX");
    }

    @Test
    void shippedCarriesCourierAndTracking() {
        assertThat(MailTemplates.render(EmailKind.SHIPPED, data("Budi", "Kopi")).text()).contains("JNE").contains("CGK123");
    }

    @Test
    void userControlledValuesAreHtmlEscaped() {
        EmailMessage m = MailTemplates.render(EmailKind.PAID, data("<script>alert(1)</script>", "Kopi <b>\"x\"</b>"));
        assertThat(m.html()).doesNotContain("<script>").doesNotContain("Kopi <b>").contains("Kopi &lt;b&gt;&quot;x&quot;&lt;/b&gt;").contains("&lt;script&gt;");
        assertThat(m.subject()).doesNotContain("script");
    }

    @Test
    void maskHidesMostOfTheAddress() {
        assertThat(EmailSender.mask("budi@example.com")).isEqualTo("bu***@example.com").doesNotContain("budi");
    }
}
