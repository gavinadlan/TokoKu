package com.tokoku.notification;

public record EmailMessage(String to, String subject, String html, String text) {}
