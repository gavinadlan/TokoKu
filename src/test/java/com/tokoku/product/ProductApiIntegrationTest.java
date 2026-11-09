package com.tokoku.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.support.ApiTestBase;

class ProductApiIntegrationTest extends ApiTestBase {

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = adminToken();
    }

    private String create(String url, String body) throws Exception {
        return mvc.perform(post(url).header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private int createCategory(String name) throws Exception {
        return JsonPath.read(create("/api/categories", "{\"name\":\"" + name + "-" + UUID.randomUUID() + "\"}"), "$.id");
    }

    private int createProduct(int categoryId, String name, int stock) throws Exception {
        String json = "{\"categoryId\":%d,\"name\":\"%s\",\"price\":10000,\"stock\":%d}".formatted(categoryId, name, stock);
        return JsonPath.read(create("/api/products", json), "$.id");
    }

    @Test
    void createThenGetDetail() throws Exception {
        int cat = createCategory("Books");
        int id = createProduct(cat, "Java Book", 7);

        mvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java Book"))
                .andExpect(jsonPath("$.stock").value(7));
    }

    @Test
    void listIsPaginatedAndFilteredByCategory() throws Exception {
        int cat = createCategory("Toys");
        for (int i = 1; i <= 3; i++) createProduct(cat, "Toy " + i, 1);

        mvc.perform(get("/api/products").param("category", String.valueOf(cat)).param("size", "2").param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Toy 3"))
                .andExpect(jsonPath("$.page.totalElements").value(3));

        mvc.perform(get("/api/products").param("category", String.valueOf(cat)).param("q", "toy 2"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void negativeStockIsRejectedWith400() throws Exception {
        int id = createProduct(createCategory("Food"), "Snack", 5);

        mvc.perform(patch("/api/products/" + id + "/stock").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"stock\":-1}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/products/" + id)).andExpect(jsonPath("$.stock").value(5));
    }

    @Test
    void deleteDeactivatesAndHidesProductFromPublicButNotAdminList() throws Exception {
        int cat = createCategory("Misc");
        int id = createProduct(cat, "Old", 1);

        mvc.perform(delete("/api/products/" + id).header("Authorization", "Bearer " + admin)).andExpect(status().isNoContent());
        mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/products").header("Authorization", "Bearer " + admin)
                .param("category", String.valueOf(cat)).param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].active").value(false));
    }
}
