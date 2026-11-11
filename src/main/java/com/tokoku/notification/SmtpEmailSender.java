package com.tokoku.notification;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.internet.MimeMessage;

public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mail;
    private final String from;

    public SmtpEmailSender(JavaMailSender mail, String from) {
        this.mail = mail;
        this.from = from;
    }

    @Override
    public void send(EmailMessage m) {
        try {
            MimeMessage mime = mail.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(mime, true, "UTF-8");
            h.setFrom(new jakarta.mail.internet.InternetAddress(from, true));
            h.setTo(m.to());
            h.setSubject(m.subject());
            h.setText(m.text(), m.html()); // multipart/alternative: teks + HTML
            for (EmailMessage.Attachment a : m.attachments()) {
                h.addAttachment(a.filename(), new org.springframework.core.io.ByteArrayResource(a.data()), a.contentType());
            }
            mail.send(mime);
        } catch (jakarta.mail.MessagingException e) {
            throw new IllegalStateException("Gagal menyusun email", e);
        }
    }
}
