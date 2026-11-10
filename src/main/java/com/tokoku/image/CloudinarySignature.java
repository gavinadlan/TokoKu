package com.tokoku.image;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tanda tangan Cloudinary (dokumentasi "Generating authentication signatures"): pasangan nama=nilai diurutkan
 * menurut nama, digabung dengan "&", ditambah API secret tanpa pemisah, lalu di-hash (SHA-1 default). Parameter
 * file, cloud_name, resource_type, dan api_key tidak ikut ditandatangani.
 */
public final class CloudinarySignature {

    private CloudinarySignature() {}

    public static String sign(Map<String, String> params, String apiSecret) {
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(params).forEach((k, v) -> {
            if (sb.length() > 0) sb.append('&');
            sb.append(k).append('=').append(v);
        });
        return sha1(sb + apiSecret);
    }

    private static String sha1(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Pembanding waktu-konstan. */
    public static boolean matches(String expected, String actual) {
        return actual != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
