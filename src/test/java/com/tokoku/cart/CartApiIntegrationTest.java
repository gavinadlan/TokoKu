package com.tokoku.cart;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.product.Product;
import com.tokoku.support.ApiTestBase;

class CartApiIntegrationTest extends ApiTestBase {

    @Test
    void cartRequiresLogin() throws Exception {
        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
    }

    @Test
    void addingSameProductAccumulatesQuantityAndTotals() throws Exception {
        String token = userToken();
        Product p = product(10, "25000");
        addToCart(token, p.getId(), 2);
        addToCart(token, p.getId(), 3);

        send(get("/api/cart"), token, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.totalQuantity").value(5))
                .andExpect(jsonPath("$.total").value(125000.0));
    }

    @Test
    void rejectsQuantityAboveStock() throws Exception {
        String token = userToken();
        Product p = product(3, "1000");
        addToCart(token, p.getId(), 2);
        send(post("/api/cart/items"), token, "{\"productId\":%d,\"quantity\":2}".formatted(p.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsInactiveOrUnknownProduct() throws Exception {
        String token = userToken();
        Product p = product(3, "1000");
        p.deactivate();
        products.save(p);
        send(post("/api/cart/items"), token, "{\"productId\":%d,\"quantity\":1}".formatted(p.getId()))
                .andExpect(status().isNotFound());
        send(post("/api/cart/items"), token, "{\"productId\":999999,\"quantity\":1}").andExpect(status().isNotFound());
        send(post("/api/cart/items"), token, "{\"productId\":1,\"quantity\":0}").andExpect(status().isBadRequest());
    }

    @Test
    void updateAndRemoveItem_andOthersCannotTouchIt() throws Exception {
        String token = userToken();
        Product p = product(10, "1000");
        addToCart(token, p.getId(), 1);
        String body = send(get("/api/cart"), token, null).andReturn().getResponse().getContentAsString();
        int itemId = JsonPath.read(body, "$.items[0].id");

        send(put("/api/cart/items/" + itemId), userToken(), "{\"quantity\":2}").andExpect(status().isNotFound());

        send(put("/api/cart/items/" + itemId), token, "{\"quantity\":4}").andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(4));
        send(put("/api/cart/items/" + itemId), token, "{\"quantity\":11}").andExpect(status().isConflict());

        send(delete("/api/cart/items/" + itemId), token, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }
}
