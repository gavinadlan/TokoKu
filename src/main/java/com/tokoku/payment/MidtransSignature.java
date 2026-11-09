package com.tokoku.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// Rumus dari dokumentasi Midtrans (HTTP notification): SHA512(order_id + status_code + gross_amount + ServerKey)
public final class MidtransSignature {

    private MidtransSignature() {}

    public static String compute(String orderId, String statusCode, String grossAmount, String serverKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hash = md.digest((orderId + statusCode + grossAmount + serverKey).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // Perbandingan waktu-konstan agar signature tidak bisa ditebak lewat selisih waktu respons
    public static boolean matches(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
