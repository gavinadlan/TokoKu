package com.tokoku.flow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.product.Product;
import com.tokoku.support.ApiTestBase;

/** Alur beli lengkap lewat HTTP: register, login, katalog, keranjang, checkout, bayar (fake), settlement, PAID. */
class FullPurchaseFlowIntegrationTest extends ApiTestBase {

    @Test
    void customerBuysAnItemFromRegisterToPaid() throws Exception {
        Product p = product(10, "85000");
        String email = uniqueEmail();

        // 1) register + login
        send(post("/api/auth/register"), null, "{\"email\":\"%s\",\"password\":\"%s\",\"name\":\"Siti\"}".formatted(email, PASSWORD))
                .andExpect(status().isCreated());
        String token = login(email, PASSWORD);
        send(get("/api/me"), token, null).andExpect(jsonPath("$.name").value("Siti"));

        // 2) lihat katalog (publik, tanpa token)
        send(get("/api/products").param("category", String.valueOf(p.getCategory().getId())), null, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(p.getId()));
        send(get("/api/products/" + p.getId()), null, null).andExpect(jsonPath("$.stock").value(10));

        // 3) keranjang
        addToCart(token, p.getId(), 2);
        send(get("/api/cart"), token, null).andExpect(jsonPath("$.total").value(170000.0));

        // 4) checkout
        long orderId = checkout(token);
        send(get("/api/orders/" + orderId), token, null).andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
        send(get("/api/products/" + p.getId()), null, null).andExpect(jsonPath("$.stock").value(8));

        // 5) bayar (gateway fake) lalu simulasi settlement
        String pay = send(post("/api/orders/" + orderId + "/pay"), token, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String ref = JsonPath.read(pay, "$.providerRef");
        send(post("/api/dev/payments/" + ref + "/simulate").param("status", "settlement"), null, null)
                .andExpect(status().isOk());

        // 6) order PAID
        send(get("/api/orders/" + orderId), token, null).andExpect(jsonPath("$.status").value("PAID"));
        send(get("/api/orders"), token, null).andExpect(jsonPath("$.content[0].status").value("PAID"));
    }
}
