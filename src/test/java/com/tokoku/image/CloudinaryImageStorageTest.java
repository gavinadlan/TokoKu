package com.tokoku.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class CloudinaryImageStorageTest {

    @Test
    void signatureMatchesTheExampleFromCloudinaryDocs() {
        // Contoh resmi (authentication_signatures): secret "abcd", timestamp 1315060510
        String signature = CloudinarySignature.sign(Map.of(
                "eager", "w_400,h_300,c_pad|w_260,h_200,c_crop", "public_id", "sample_image", "timestamp", "1315060510"), "abcd");
        assertThat(signature).isEqualTo("bfd09f95f331f558cbd1320e67aa8d488770583e");
    }

    private CloudinaryImageStorage storage(String base) {
        return new CloudinaryImageStorage("democloud", "key123", "secret456", base);
    }

    @Test
    void signUploadSignsFolderFormatsAndTimestampWithoutLeakingTheSecret() {
        UploadSignature s = storage("https://api.cloudinary.com").signUpload();
        String expected = CloudinarySignature.sign(Map.of("allowed_formats", "jpg,png,webp", "folder", "tokoku/products",
                "timestamp", String.valueOf(s.timestamp())), "secret456");
        assertThat(s.signature()).isEqualTo(expected);
        assertThat(s.uploadUrl()).isEqualTo("https://api.cloudinary.com/v1_1/democloud/image/upload");
        assertThat(s.apiKey()).isEqualTo("key123");
        assertThat(s.toString()).doesNotContain("secret456");
    }

    @Test
    void verifiesUploadResponseSignatureAndRejectsForgeries() {
        CloudinaryImageStorage st = storage("https://api.cloudinary.com");
        String sig = CloudinarySignature.sign(Map.of("public_id", "tokoku/products/abc", "version", "1719307544"), "secret456");
        assertThat(st.verifyUploadResponse("tokoku/products/abc", 1719307544L, sig)).isTrue();
        assertThat(st.verifyUploadResponse("tokoku/products/abc", 1719307545L, sig)).isFalse();
        assertThat(st.verifyUploadResponse("tokoku/products/other", 1719307544L, sig)).isFalse();
        assertThat(st.verifyUploadResponse("tokoku/products/abc", 1719307544L, null)).isFalse();
    }

    @Test
    void buildsNamedSizeUrlsWithFixedTransformation() {
        CloudinaryImageStorage st = storage("https://api.cloudinary.com");
        String url = st.originalUrl("tokoku/products/abc", 1719307544L, "jpg");
        assertThat(url).isEqualTo("https://res.cloudinary.com/democloud/image/upload/v1719307544/tokoku/products/abc.jpg");
        assertThat(st.variantUrl(url, "tokoku/products/abc", ImageSize.CARD))
                .isEqualTo("https://res.cloudinary.com/democloud/image/upload/f_auto,q_auto,c_fill,ar_4:5,w_480/v1719307544/tokoku/products/abc.jpg");
        assertThat(st.variantUrl(url, "tokoku/products/abc", ImageSize.THUMBNAIL)).contains("w_160");
        assertThat(st.variantUrl(url, "tokoku/products/abc", ImageSize.DETAIL)).contains("w_960");
        assertThat(st.variantUrl(url, "tokoku/products/abc", ImageSize.LARGE)).contains("c_fill,ar_4:5,w_1600");
    }

    @Test
    void externalImagesAreReturnedAsIs() {
        CloudinaryImageStorage st = storage("https://api.cloudinary.com");
        assertThat(st.variantUrl("https://cdn.example.com/kopi.jpg", null, ImageSize.CARD)).isEqualTo("https://cdn.example.com/kopi.jpg");
    }

    @Test
    void missingCredentialsFailFastWithClearMessage() {
        assertThatThrownBy(() -> new CloudinaryImageStorage("", "", "", "https://x")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOUDINARY_API_SECRET");
    }

    @Test
    void deleteSendsBasicAuthAndNeverThrowsOnFailure() throws Exception {
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            auth.set(ex.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            ex.sendResponseHeaders(500, -1); // Cloudinary gagal
            ex.close();
        });
        server.start();
        try {
            CloudinaryImageStorage st = storage("http://127.0.0.1:" + server.getAddress().getPort());
            assertThatCode(() -> st.delete("tokoku/products/abc")).doesNotThrowAnyException();
            assertThat(auth.get()).startsWith("Basic ");
            assertThat(body.get()).contains("public_id=tokoku%2Fproducts%2Fabc");
            assertThatCode(() -> storage("http://127.0.0.1:1").delete("x")).doesNotThrowAnyException(); // tidak terjangkau
        } finally {
            server.stop(0);
        }
    }
}
