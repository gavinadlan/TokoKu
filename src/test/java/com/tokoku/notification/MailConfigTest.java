package com.tokoku.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;

class MailConfigTest {
    private static final String FROM = "TokoKu <a@b.c>";
    private final MailConfig cfg = new MailConfig();
    private final org.springframework.beans.factory.ObjectProvider<JavaMailSender> mail =
            new StaticListableBeanFactory().getBeanProvider(JavaMailSender.class);

    private EmailSender make(boolean enabled, String provider, String user, String pass, String key) {
        return cfg.emailSender(mail, enabled, provider, user, pass, FROM, key, "https://x.invalid/v3/smtp/email");
    }

    @Test
    void blankCredentialsOrDisabledGiveNoop() {
        assertThat(make(true, "smtp", "", "", "")).isInstanceOf(NoopEmailSender.class);
        assertThat(make(false, "smtp", "u", "p", "k")).isInstanceOf(NoopEmailSender.class);
        assertThat(make(true, "smtp", "u", "p", "").delivers()).isFalse(); // tanpa JavaMailSender juga no-op
        assertThat(make(true, "noop", "u", "p", "k")).isInstanceOf(NoopEmailSender.class);
        assertThat(make(true, "brevo", "", "", "")).isInstanceOf(NoopEmailSender.class);
    }

    @Test
    void brevoWithKeyAndUnknownProvider() {
        assertThat(make(true, "BREVO", "", "", "k")).isInstanceOf(BrevoEmailSender.class);
        assertThatThrownBy(() -> make(true, "sendgrid", "", "", "k")).isInstanceOf(IllegalStateException.class);
    }
}
