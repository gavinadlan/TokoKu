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

@Configuration
@EnableAsync
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    /** SMTP hanya bila diaktifkan DAN username + password terisi; selain itu no-op. */
    @Bean
    EmailSender emailSender(ObjectProvider<JavaMailSender> mail,
            @Value("${app.mail.enabled:true}") boolean enabled,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${app.mail.from:TokoKu <noreply@tokoku.local>}") String from) {
        JavaMailSender sender = mail.getIfAvailable();
        if (!enabled || username.isBlank() || password.isBlank() || sender == null) {
            log.info("Email: no-op ({})", !enabled ? "APP_MAIL_ENABLED=false" : "MAIL_USERNAME/MAIL_PASSWORD kosong");
            return new NoopEmailSender();
        }
        log.info("Email: SMTP aktif (host dari konfigurasi, kredensial tidak dicetak)");
        return new SmtpEmailSender(sender, from);
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
