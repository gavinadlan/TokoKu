package com.tokoku.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Meniru endpoint unggah Cloudinary untuk dev dan e2e (provider fake): menerima berkas, menyimpannya di memori
 * (terbatas), dan membalas dengan bentuk respons yang sama. Tidak ada di profil prod.
 */
@Hidden
@RestController
@Profile("!prod")
@ConditionalOnProperty(name = "app.images.provider", havingValue = "fake")
@RequestMapping(FakeImageStorage.UPLOAD_PATH)
public class DevUploadController {

    private static final int MAX_FILES = 60;
    private static final Set<String> FORMATS = Set.of("jpg", "jpeg", "png", "webp");

    private record Stored(byte[] bytes, String contentType) {}

    private final FakeImageStorage storage;
    private final Map<String, Stored> files = java.util.Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Stored> eldest) { return size() > MAX_FILES; }
    });

    public DevUploadController(FakeImageStorage storage) {
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file, @RequestParam("timestamp") long timestamp,
            @RequestParam("signature") String signature) throws IOException {
        if (!storage.verifyRequest(timestamp, signature)) {
            return error("Invalid Signature");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        if (!FORMATS.contains(ext)) {
            return error("Image format not allowed. Allowed: jpg, png, webp");
        }
        String format = ext.equals("jpeg") ? "jpg" : ext;
        byte[] bytes = file.getBytes();
        int w = 0, h = 0;
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        if (img != null) { w = img.getWidth(); h = img.getHeight(); }
        String id = UUID.randomUUID().toString();
        String publicId = ImageStorage.FOLDER + "/" + id;
        long version = Instant.now().getEpochSecond();
        files.put(id, new Stored(bytes, "image/" + (format.equals("jpg") ? "jpeg" : format)));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("public_id", publicId);
        body.put("version", version);
        body.put("signature", storage.responseSignature(publicId, version));
        body.put("width", w);
        body.put("height", h);
        body.put("format", format);
        body.put("bytes", bytes.length);
        body.put("secure_url", storage.originalUrl(publicId, version, format));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/files/{name}")
    public ResponseEntity<byte[]> file(@PathVariable String name) {
        String id = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        Stored s = files.get(id);
        if (s == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(s.contentType())).header("Cache-Control", "max-age=300").body(s.bytes());
    }

    private static ResponseEntity<Map<String, Object>> error(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", Map.of("message", message)));
    }
}
