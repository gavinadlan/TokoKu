package com.tokoku.image;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** Untuk test dan dev tanpa kunci. Tanpa jaringan; di dev, berkas diterima oleh DevUploadController. Dilarang di prod. */
@Component
@ConditionalOnProperty(name = "app.images.provider", havingValue = "fake")
public class FakeImageStorage implements ImageStorage {

    public static final String UPLOAD_PATH = "/api/dev/uploads";

    private final String secret;
    private final String publicBaseUrl;
    private final List<String> deleted = new CopyOnWriteArrayList<>();

    public FakeImageStorage(Environment env, @Value("${app.images.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        if (env.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("app.images.provider=fake is not allowed in the prod profile");
        }
        byte[] random = new byte[24];
        new SecureRandom().nextBytes(random);
        this.secret = HexFormat.of().formatHex(random);
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public UploadSignature signUpload() {
        long ts = Instant.now().getEpochSecond();
        return new UploadSignature(UPLOAD_PATH, "fake", ts, FOLDER, ALLOWED_FORMATS, requestSignature(ts), "fake");
    }

    String requestSignature(long ts) {
        return CloudinarySignature.sign(Map.of("allowed_formats", ALLOWED_FORMATS, "folder", FOLDER, "timestamp", String.valueOf(ts)), secret);
    }

    boolean verifyRequest(long ts, String signature) {
        return CloudinarySignature.matches(requestSignature(ts), signature);
    }

    /** Signature respons unggah yang sah (dipakai DevUploadController dan test). */
    public String responseSignature(String publicId, long version) {
        return CloudinarySignature.sign(Map.of("public_id", publicId, "version", String.valueOf(version)), secret);
    }

    @Override
    public boolean verifyUploadResponse(String publicId, long version, String signature) {
        return CloudinarySignature.matches(responseSignature(publicId, version), signature);
    }

    @Override
    public String originalUrl(String publicId, long version, String format) {
        String id = publicId.substring(publicId.lastIndexOf('/') + 1);
        return publicBaseUrl + UPLOAD_PATH + "/files/" + id + "." + format;
    }

    @Override
    public String variantUrl(String storedUrl, String publicId, ImageSize size) {
        return storedUrl; // tanpa transformasi
    }

    @Override
    public void delete(String publicId) {
        deleted.add(publicId);
    }

    /** Untuk test: aset yang diminta dihapus. */
    public List<String> deleted() { return deleted; }
}
