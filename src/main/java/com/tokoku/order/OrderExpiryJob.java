package com.tokoku.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Pembatalan berkala. Di hosting yang tidur saat idle, pengecekan "malas" di OrderService menutup celahnya.
@Component
public class OrderExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);

    private final OrderService service;

    public OrderExpiryJob(OrderService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.order.expiry-check-ms:300000}", initialDelayString = "${app.order.expiry-check-ms:300000}")
    public void run() {
        int n = service.expireOverdue();
        if (n > 0) {
            log.info("Expired {} unpaid orders", n);
        }
    }
}
