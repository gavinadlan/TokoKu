package com.tokoku.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

import com.tokoku.product.Product;
import com.tokoku.support.ApiTestBase;

class OrderApiIntegrationTest extends ApiTestBase {

    private int stockOf(Product p) {
        return products.findById(p.getId()).orElseThrow().getStock();
    }

    @Test
    void checkoutCopiesPriceReducesStockAndClearsCart() throws Exception {
        String token = userToken();
        Product p = product(10, "20000");
        addToCart(token, p.getId(), 3);

        long orderId = checkout(token);

        assertThat(stockOf(p)).isEqualTo(7);
        send(get("/api/orders/" + orderId), token, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmount").value(60000.0))
                .andExpect(jsonPath("$.items[0].unitPrice").value(20000.0))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.shippingName").value("Budi"));
        send(get("/api/cart"), token, null).andExpect(jsonPath("$.items.length()").value(0));

        // harga produk berubah sesudah checkout: order lama tetap memakai harga salinan
        p = products.findById(p.getId()).orElseThrow();
        p.update(p.getCategory(), p.getName(), "x", new java.math.BigDecimal("99999"));
        products.save(p);
        send(get("/api/orders/" + orderId), token, null).andExpect(jsonPath("$.items[0].unitPrice").value(20000.0));
    }

    @Test
    void emptyCartIs400() throws Exception {
        String token = userToken();
        send(post("/api/orders"), token, SHIPPING).andExpect(status().isBadRequest());
    }

    @Test
    void insufficientStockIs409AndNothingChanges() throws Exception {
        String token = userToken();
        Product ok = product(5, "1000");
        Product scarce = product(2, "1000");
        addToCart(token, ok.getId(), 1);
        addToCart(token, scarce.getId(), 2);
        // stok dihabiskan orang lain setelah masuk keranjang
        scarce.setStock(1);
        products.save(scarce);

        send(post("/api/orders"), token, SHIPPING).andExpect(status().isConflict());

        assertThat(stockOf(ok)).isEqualTo(5);      // rollback: produk pertama tidak berkurang
        assertThat(stockOf(scarce)).isEqualTo(1);
        send(get("/api/cart"), token, null).andExpect(jsonPath("$.items.length()").value(2)); // keranjang utuh
    }

    @Test
    void shippingIsValidated() throws Exception {
        String token = userToken();
        Product p = product(5, "1000");
        addToCart(token, p.getId(), 1);
        send(post("/api/orders"), token, "{\"shippingName\":\"\",\"shippingPhone\":\"1\",\"shippingAddress\":\"x\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void otherUsersOrderIs404() throws Exception {
        String owner = userToken();
        Product p = product(5, "1000");
        addToCart(owner, p.getId(), 1);
        long orderId = checkout(owner);

        send(get("/api/orders/" + orderId), userToken(), null).andExpect(status().isNotFound());
        send(get("/api/orders"), userToken(), null).andExpect(jsonPath("$.content.length()").value(0));
        send(get("/api/orders"), owner, null).andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void concurrentCheckoutNeverOversells() throws Exception {
        Product p = product(1, "5000");
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String t = userToken();
            addToCart(t, p.getId(), 1);
            tokens.add(t);
        }

        ExecutorService pool = Executors.newFixedThreadPool(tokens.size());
        CyclicBarrier barrier = new CyclicBarrier(tokens.size());
        List<Future<Integer>> results = new ArrayList<>();
        for (String t : tokens) {
            results.add(pool.submit(() -> {
                barrier.await(); // semua thread mulai bersamaan
                return send(post("/api/orders"), t, SHIPPING).andReturn().getResponse().getStatus();
            }));
        }
        List<Integer> codes = new ArrayList<>();
        for (Future<Integer> f : results) codes.add(f.get(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertThat(codes.stream().filter(c -> c == 201)).hasSize(1);
        assertThat(codes.stream().filter(c -> c == 409)).hasSize(tokens.size() - 1);
        assertThat(stockOf(p)).isZero();
    }

    @Test
    void adminListsFiltersAndAdvancesStatus() throws Exception {
        String user = userToken();
        String admin = adminToken();
        Product p = product(5, "1000");
        addToCart(user, p.getId(), 1);
        long id = checkout(user);

        send(get("/api/admin/orders").param("status", "PENDING_PAYMENT"), user, null).andExpect(status().isForbidden());
        send(get("/api/admin/orders").param("status", "PENDING_PAYMENT").param("size", "100"), admin, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[?(@.id==" + id + ")]").exists());
        send(get("/api/admin/orders").param("status", "SHIPPED").param("size", "100"), admin, null)
                .andExpect(jsonPath("$.content[?(@.id==" + id + ")]").doesNotExist());

        // PENDING_PAYMENT -> SHIPPED tidak sah
        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"SHIPPED\"}").andExpect(status().isConflict());
        // PAID tidak boleh diset admin (hanya lewat pembayaran)
        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"PAID\"}").andExpect(status().isConflict());
        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"NOPE\"}").andExpect(status().isBadRequest());
    }
}
