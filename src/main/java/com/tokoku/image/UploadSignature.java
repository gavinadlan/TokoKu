package com.tokoku.image;

/** Parameter yang dikirim browser langsung ke penyimpanan gambar (file tidak melewati server kita). */
public record UploadSignature(String uploadUrl, String apiKey, long timestamp, String folder, String allowedFormats,
        String signature, String provider) {}
