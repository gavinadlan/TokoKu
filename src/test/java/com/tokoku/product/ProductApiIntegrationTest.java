package com.tokoku.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductApiIntegrationTest {

    @Autowired MockMvc mvc;

    private String create(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private int createCategory(String name) throws Exception {
        return JsonPath.read(create("/api/categories", "{\"name\":\"" + name + "\"}"), "$.id");
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
                .andExpect(jsonPath("$.stock").value(7))
                .andExpect(jsonPath("$.categoryName").value("Books"));
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

        mvc.perform(patch("/api/products/" + id + "/stock").contentType(MediaType.APPLICATION_JSON).content("{\"stock\":-1}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/products/" + id)).andExpect(jsonPath("$.stock").value(5));
    }

    @Test
    void deleteDeactivatesAndHidesProduct() throws Exception {
        int id = createProduct(createCategory("Misc"), "Old", 1);

        mvc.perform(delete("/api/products/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
    }
}
