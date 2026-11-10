package com.tokoku.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.image.FakeImageStorage;
import com.tokoku.support.ApiTestBase;

/** Gate B3: unggah (didaftarkan), urutan, foto utama, dan hapus lewat API, dengan penyimpanan palsu tanpa jaringan. */
class ProductImageApiIntegrationTest extends ApiTestBase {

    @Autowired FakeImageStorage storage;

    private String publicId() { return "tokoku/products/" + UUID.randomUUID(); }

    private String register(String admin, long productId, String publicId, long version, String format, long bytes, int expected) throws Exception {
        String sig = storage.responseSignature(publicId, version);
        return send(post("/api/admin/products/" + productId + "/images"), admin,
                "{\"publicId\":\"%s\",\"version\":%d,\"signature\":\"%s\",\"width\":800,\"height\":1000,\"format\":\"%s\",\"bytes\":%d}"
                        .formatted(publicId, version, sig, format, bytes))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
    }

    private long add(String admin, long productId) throws Exception {
        String body = register(admin, productId, publicId(), 1719307544L, "jpg", 400_000, 200);
        List<Integer> ids = JsonPath.read(body, "$.images[*].id");
        return ids.get(ids.size() - 1);
    }

    @Test
    void signingIsAdminOnlyAndNeverReturnsASecret() throws Exception {
        send(post("/api/admin/uploads/sign"), null, null).andExpect(status().isUnauthorized());
        send(post("/api/admin/uploads/sign"), userToken(), null).andExpect(status().isForbidden());
        String body = send(post("/api/admin/uploads/sign"), adminToken(), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.signature").isNotEmpty()).andExpect(jsonPath("$.folder").value("tokoku/products"))
                .andExpect(jsonPath("$.allowedFormats").value("jpg,png,webp")).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContainIgnoringCase("secret");
    }

    @Test
    void registerForgedSignatureIsRejected() throws Exception {
        String admin = adminToken();
        long pid = product(1, "1000").getId();
        String id = publicId();
        send(post("/api/admin/products/" + pid + "/images"), admin,
                "{\"publicId\":\"%s\",\"version\":1,\"signature\":\"%s\",\"format\":\"jpg\",\"bytes\":1000}".formatted(id, "0".repeat(40)))
                .andExpect(status().isBadRequest());
        // publicId di luar folder kita ditolak walau signature sah
        register(admin, pid, "other/folder/x", 1, "jpg", 1000, 400);
        // user biasa tidak boleh
        send(post("/api/admin/products/" + pid + "/images"), userToken(), "{}").andExpect(status().isForbidden());
    }

    @Test
    void firstImageIsPrimaryAndProductResponseCarriesNamedSizeUrls() throws Exception {
        String admin = adminToken();
        long pid = product(1, "1000").getId();
        String body = register(admin, pid, publicId(), 1719307544L, "jpg", 400_000, 200);

        assertThat((Boolean) JsonPath.read(body, "$.images[0].primary")).isTrue();
        assertThat((String) JsonPath.read(body, "$.images[0].thumbnailUrl")).isNotBlank();
        assertThat((String) JsonPath.read(body, "$.images[0].cardUrl")).isNotBlank();
        assertThat((String) JsonPath.read(body, "$.images[0].detailUrl")).isNotBlank();
        assertThat((String) JsonPath.read(body, "$.imageUrl")).isEqualTo(JsonPath.read(body, "$.images[0].cardUrl"));
        // publik melihat foto yang sama
        mvc.perform(get("/api/products/" + pid)).andExpect(status().isOk()).andExpect(jsonPath("$.images.length()").value(1));
        // admin bisa membaca produk nonaktif
        send(get("/api/admin/products/" + pid), admin, null).andExpect(status().isOk()).andExpect(jsonPath("$.images.length()").value(1));
    }

    @Test
    void maximumSixImagesAndFormatAndSizeLimits() throws Exception {
        String admin = adminToken();
        long pid = product(1, "1000").getId();
        for (int i = 0; i < 6; i++) add(admin, pid);
        register(admin, pid, publicId(), 1719307544L, "jpg", 1000, 409);

        long pid2 = product(1, "1000").getId();
        register(admin, pid2, publicId(), 1, "gif", 1000, 400);
        String tooBig = publicId();
        register(admin, pid2, tooBig, 1, "jpg", 5L * 1024 * 1024 + 1, 400);
        assertThat(storage.deleted()).contains(tooBig); // aset yang melanggar aturan ikut dibersihkan
        register(admin, pid2, publicId(), 1, "webp", 5L * 1024 * 1024, 200); // tepat 5 MB boleh
    }

    @Test
    void reorderMakePrimaryAndDelete() throws Exception {
        String admin = adminToken();
        long pid = product(1, "1000").getId();
        long a = add(admin, pid), b = add(admin, pid), c = add(admin, pid);

        // urutan baru c, a, b
        String reordered = send(put("/api/admin/products/" + pid + "/images/order"), admin, "{\"imageIds\":[%d,%d,%d]}".formatted(c, a, b))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat((List<Integer>) JsonPath.read(reordered, "$.images[*].id")).containsExactly((int) c, (int) a, (int) b);
        // daftar tidak lengkap ditolak
        send(put("/api/admin/products/" + pid + "/images/order"), admin, "{\"imageIds\":[%d,%d]}".formatted(a, b)).andExpect(status().isBadRequest());

        // jadikan c utama
        String primary = send(patch("/api/admin/products/" + pid + "/images/" + c + "/primary"), admin, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((List<Boolean>) JsonPath.read(primary, "$.images[*].primary")).containsExactly(true, false, false);
        send(patch("/api/admin/products/" + pid + "/images/999999/primary"), admin, null).andExpect(status().isNotFound());

        // hapus foto utama: foto berikutnya jadi utama, posisi dirapatkan, aset dihapus di penyimpanan
        String afterDelete = send(delete("/api/admin/products/" + pid + "/images/" + c), admin, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((List<Integer>) JsonPath.read(afterDelete, "$.images[*].id")).containsExactly((int) a, (int) b);
        assertThat((List<Boolean>) JsonPath.read(afterDelete, "$.images[*].primary")).containsExactly(true, false);
        assertThat((List<Integer>) JsonPath.read(afterDelete, "$.images[*].position")).containsExactly(0, 1);
        assertThat(storage.deleted()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void legacyExternalImageIsServedAsIs() throws Exception {
        long pid = product(1, "1000").getId();
        jdbcInsertExternal(pid, "https://cdn.example.com/kopi.jpg");
        mvc.perform(get("/api/products/" + pid)).andExpect(status().isOk())
                .andExpect(jsonPath("$.images[0].cardUrl").value("https://cdn.example.com/kopi.jpg"))
                .andExpect(jsonPath("$.images[0].thumbnailUrl").value("https://cdn.example.com/kopi.jpg"))
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.example.com/kopi.jpg"));
    }

    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    private void jdbcInsertExternal(long productId, String url) {
        jdbc.update("insert into product_images (product_id, public_id, url, position, is_primary) values (?, null, ?, 0, true)", productId, url);
    }
}
