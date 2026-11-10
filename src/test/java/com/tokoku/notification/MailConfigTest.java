package com.tokoku.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MailConfigTest {
    @Test
    void blankCredentialsOrDisabledGiveNoop() {
        var cfg = new MailConfig();
        var mail = new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider(org.springframework.mail.javamail.JavaMailSender.class);
        assertThat(cfg.emailSender(mail, true, "", "", "TokoKu <a@b.c>")).isInstanceOf(NoopEmailSender.class);
        assertThat(cfg.emailSender(mail, false, "u", "p", "TokoKu <a@b.c>")).isInstanceOf(NoopEmailSender.class);
        assertThat(cfg.emailSender(mail, true, "u", "p", "TokoKu <a@b.c>").delivers()).isFalse(); // tanpa JavaMailSender juga no-op
    }
}
