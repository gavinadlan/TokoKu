package com.tokoku.notification;

import java.util.List;

public record EmailMessage(String to, String subject, String html, String text, List<Attachment> attachments) {

    public record Attachment(String filename, String contentType, byte[] data) {}

    public EmailMessage(String to, String subject, String html, String text) {
        this(to, subject, html, text, List.of());
    }

    public EmailMessage withAttachments(List<Attachment> a) {
        return new EmailMessage(to, subject, html, text, a);
    }
}
