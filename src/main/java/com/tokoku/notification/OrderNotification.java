package com.tokoku.notification;

/** Dipublikasikan di dalam transaksi perubahan status; listener baru jalan setelah commit. */
public record OrderNotification(Long orderId, EmailKind kind) {}
