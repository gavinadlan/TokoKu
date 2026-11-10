package com.tokoku.order;

import java.security.SecureRandom;
import java.util.Locale;

/** Kode ambil 6 karakter yang mudah dibaca: tanpa 0/O dan 1/I/L. Pencocokan tidak peka huruf besar-kecil. */
public final class PickupCodes {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PickupCodes() {}

    public static String generate() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        return sb.toString();
    }

    /** Huruf besar, tanpa spasi dan tanda hubung (pembeli bisa menulis "k7m 2px"). */
    public static String normalize(String input) {
        return input == null ? "" : input.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }
}
