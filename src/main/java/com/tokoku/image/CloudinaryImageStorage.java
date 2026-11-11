package com.tokoku.image;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Cloudinary (paket gratis). Unggah langsung dari browser dengan tanda tangan; server memverifikasi respons
 * dan membangun URL ukuran bernama dengan f_auto,q_auto,c_fill,ar_4:5.
 *
 * Terverifikasi dari dokumentasi resmi: rumus tanda tangan unggah, URL unggah, parameter allowed_formats dan folder,
 * autentikasi Basic untuk API. BELUM terverifikasi dari dokumentasi (halaman tidak memuatnya): rumus signature respons
 * unggah (dipakai: algoritma yang sama atas "public_id=...&version=...", sesuai perilaku SDK Cloudinary) dan sintaks
 * transformasi URL (sintaks standar dari pengetahuan umum).
 */
@Component
@ConditionalOnProperty(name = "app.images.provider", havingValue = "cloudinary", matchIfMissing = true)
public class CloudinaryImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryImageStorage.class);

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String apiBase;
    private final RestClient http;

    public CloudinaryImageStorage(
            @Value("${app.images.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.images.cloudinary.api-key:}") String apiKey,
            @Value("${app.images.cloudinary.api-secret:}") String apiSecret,
            @Value("${app.images.cloudinary.api-base:https://api.cloudinary.com}") String apiBase) {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new IllegalStateException("app.images.provider=cloudinary requires CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and "
                    + "CLOUDINARY_API_SECRET (or set APP_IMAGES_PROVIDER=fake for local development without Cloudinary)");
        }
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.apiBase = apiBase;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public UploadSignature signUpload() {
        long ts = Instant.now().getEpochSecond();
        String signature = CloudinarySignature.sign(
                Map.of("allowed_formats", ALLOWED_FORMATS, "folder", FOLDER, "timestamp", String.valueOf(ts)), apiSecret);
        return new UploadSignature(apiBase + "/v1_1/" + cloudName + "/image/upload", apiKey, ts, FOLDER, ALLOWED_FORMATS, signature, "cloudinary");
    }

    @Override
    public boolean verifyUploadResponse(String publicId, long version, String signature) {
        return CloudinarySignature.matches(
                CloudinarySignature.sign(Map.of("public_id", publicId, "version", String.valueOf(version)), apiSecret), signature);
    }

    @Override
    public String originalUrl(String publicId, long version, String format) {
        return "https://res.cloudinary.com/" + cloudName + "/image/upload/v" + version + "/" + publicId + "." + format;
    }

    @Override
    public String variantUrl(String storedUrl, String publicId, ImageSize size) {
        if (publicId == null || !storedUrl.contains("/image/upload/")) {
            return storedUrl;
        }
        String transform = size.crop()
                ? "f_auto,q_auto,c_fill,ar_4:5,w_" + size.width()
                : "f_auto,q_auto,c_limit,w_" + size.width() + ",h_" + size.width();
        return storedUrl.replaceFirst("/image/upload/", "/image/upload/" + transform + "/");
    }

    @Override
    public void delete(String publicId) {
        if (publicId == null) return;
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("public_id", publicId);
            http.post().uri(URI.create(apiBase + "/v1_1/" + cloudName + "/image/destroy"))
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((apiKey + ":" + apiSecret).getBytes(StandardCharsets.UTF_8)))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().toBodilessEntity();
        } catch (Exception e) {
            // Best-effort: aset yatim di Cloudinary tidak boleh menggagalkan permintaan admin
            log.warn("Could not delete Cloudinary asset {}: {}", publicId, e.getClass().getSimpleName());
        }
    }
}
