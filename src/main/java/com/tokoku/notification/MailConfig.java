package com.tokoku.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

@Configuration
@EnableAsync
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    /**
     * APP_MAIL_PROVIDER: smtp (default) | brevo (API HTTPS, untuk host yang memblokir SMTP) | noop.
     * Kredensial kosong atau APP_MAIL_ENABLED=false = no-op; kunci tidak pernah dicetak.
     */
    @Bean
    EmailSender emailSender(ObjectProvider<JavaMailSender> mail,
            @Value("${app.mail.enabled:true}") boolean enabled,
            @Value("${app.mail.provider:smtp}") String provider,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${app.mail.from:TokoKu <noreply@tokoku.local>}") String from,
            @Value("${app.mail.brevo.api-key:}") String brevoKey,
            @Value("${app.mail.brevo.url:https://api.brevo.com/v3/smtp/email}") String brevoUrl) {
        String p = provider.strip().toLowerCase();
        if (!enabled || p.equals("noop")) {
            log.info("Email: no-op ({})", !enabled ? "APP_MAIL_ENABLED=false" : "APP_MAIL_PROVIDER=noop");
            return new NoopEmailSender();
        }
        switch (p) {
            case "brevo" -> {
                if (brevoKey.isBlank()) {
                    log.warn("Email: no-op (APP_MAIL_PROVIDER=brevo tetapi BREVO_API_KEY kosong)");
                    return new NoopEmailSender();
                }
                log.info("Email: Brevo API aktif (kunci tidak dicetak)");
                return new BrevoEmailSender(RestClient.builder().requestFactory(timeouts()), brevoUrl, brevoKey, from);
            }
            case "smtp" -> {
                JavaMailSender sender = mail.getIfAvailable();
                if (username.isBlank() || password.isBlank() || sender == null) {
                    log.info("Email: no-op (MAIL_USERNAME/MAIL_PASSWORD kosong)");
                    return new NoopEmailSender();
                }
                log.info("Email: SMTP aktif (host dari konfigurasi, kredensial tidak dicetak)");
                return new SmtpEmailSender(sender, from);
            }
            default -> throw new IllegalStateException("APP_MAIL_PROVIDER harus smtp, brevo, atau noop");
        }
    }

    private static org.springframework.http.client.JdkClientHttpRequestFactory timeouts() {
        var f = new org.springframework.http.client.JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build());
        f.setReadTimeout(java.time.Duration.ofSeconds(10));
        return f;
    }

    /** Thread pool kecil khusus email: lonjakan pesanan tidak menahan request web. */
    @Bean("mailExecutor")
    ThreadPoolTaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(2);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("mail-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(10);
        return ex;
    }
}
