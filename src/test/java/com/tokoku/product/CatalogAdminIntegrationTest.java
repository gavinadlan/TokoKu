package com.tokoku.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.support.ApiTestBase;

class CatalogAdminIntegrationTest extends ApiTestBase {

    private int newCategory(String admin) throws Exception {
        String body = send(post("/api/categories"), admin, "{\"name\":\"cat-%s\"}".formatted(UUID.randomUUID())).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void categoryRenameAndDelete() throws Exception {
        String admin = adminToken();
        int id = newCategory(admin);
        String renamed = "renamed-" + UUID.randomUUID();

        send(put("/api/categories/" + id), admin, "{\"name\":\"%s\"}".formatted(renamed)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed));
        send(put("/api/categories/" + id), admin, "{\"name\":\"\"}").andExpect(status().isBadRequest());
        send(put("/api/categories/999999"), admin, "{\"name\":\"x\"}").andExpect(status().isNotFound());

        send(delete("/api/categories/" + id), userToken(), null).andExpect(status().isForbidden());
        send(delete("/api/categories/" + id), null, null).andExpect(status().isUnauthorized());
        send(delete("/api/categories/" + id), admin, null).andExpect(status().isNoContent());
        send(delete("/api/categories/" + id), admin, null).andExpect(status().isNotFound());
    }

    @Test
    void duplicateCategoryNameIs409() throws Exception {
        String admin = adminToken();
        int first = newCategory(admin);
        int second = newCategory(admin);
        String firstName = categories.findById((long) first).orElseThrow().getName();
        send(put("/api/categories/" + second), admin, "{\"name\":\"%s\"}".formatted(firstName)).andExpect(status().isConflict());
    }

    @Test
    void cannotDeleteCategoryThatHasProducts_evenInactiveOnes() throws Exception {
        String admin = adminToken();
        var p = product(1, "1000");
        long catId = p.getCategory().getId();
        send(delete("/api/categories/" + catId), admin, null).andExpect(status().isConflict());

        send(delete("/api/products/" + p.getId()), admin, null).andExpect(status().isNoContent()); // nonaktif, baris tetap ada
        send(delete("/api/categories/" + catId), admin, null).andExpect(status().isConflict());
    }
}
