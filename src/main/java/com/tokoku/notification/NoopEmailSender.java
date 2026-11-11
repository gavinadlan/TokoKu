package com.tokoku.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoopEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(NoopEmailSender.class);

    @Override
    public boolean delivers() { return false; }

    @Override
    public void send(EmailMessage m) {
        log.info("Email dilewati (pengirim no-op): ke={} subjek={}", EmailSender.mask(m.to()), m.subject());
    }
}
