package com.tokoku.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.payment.PaymentRepository;
import com.tokoku.payment.PaymentStatus;
import com.tokoku.product.Product;
import com.tokoku.support.ApiTestBase;

class OrderLifecycleIntegrationTest extends ApiTestBase {

    @Autowired JdbcTemplate jdbc;
    @Autowired OrderExpiryJob job;
    @Autowired OrderService orderService;
    @Autowired PaymentRepository payments;

    private int stock(Product p) { return products.findById(p.getId()).orElseThrow().getStock(); }

    private void backdate(long orderId, int hours) {
        jdbc.update("update orders set created_at = now() - make_interval(hours => ?) where id = ?", hours, orderId);
    }

    private String statusOf(String token, long orderId) throws Exception {
        String b = send(get("/api/orders/" + orderId), token, null).andReturn().getResponse().getContentAsString();
        return JsonPath.read(b, "$.status");
    }

    private long orderOf(String token, Product p, int qty) throws Exception {
        addToCart(token, p.getId(), qty);
        return checkout(token);
    }

    @Test
    void ownerCancelRestoresStockAndClosesPendingPayment() throws Exception {
        String token = userToken();
        Product p = product(5, "10000");
        long id = orderOf(token, p, 2);
        send(post("/api/orders/" + id + "/pay"), token, null).andExpect(status().isOk());
        assertThat(stock(p)).isEqualTo(3);

        send(post("/api/orders/" + id + "/cancel"), token, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(stock(p)).isEqualTo(5);
        assertThat(payments.findByOrderId(id)).allMatch(x -> x.getStatus() == PaymentStatus.EXPIRED);
        send(post("/api/orders/" + id + "/cancel"), token, null).andExpect(status().isConflict()); // tidak dobel
        assertThat(stock(p)).isEqualTo(5);
    }

    @Test
    void cannotCancelOthersOrPaidOrders() throws Exception {
        String token = userToken();
        Product p = product(5, "10000");
        long id = orderOf(token, p, 1);

        send(post("/api/orders/" + id + "/cancel"), userToken(), null).andExpect(status().isNotFound());

        jdbc.update("update orders set status = 'PAID' where id = ?", id);
        send(post("/api/orders/" + id + "/cancel"), token, null).andExpect(status().isConflict());
        assertThat(stock(p)).isEqualTo(4);
    }

    @Test
    void lazyExpiryOnReadCancelsOverdueOrderAndRestocks() throws Exception {
        String token = userToken();
        Product p = product(5, "10000");
        long id = orderOf(token, p, 2);
        backdate(id, 3);

        assertThat(statusOf(token, id)).isEqualTo("CANCELLED");
        assertThat(stock(p)).isEqualTo(5);
        assertThat(statusOf(token, id)).isEqualTo("CANCELLED");
        assertThat(stock(p)).isEqualTo(5); // pembacaan kedua tidak mengembalikan stok lagi
    }

    @Test
    void lazyExpiryOnListAndSchedulerJob() throws Exception {
        String token = userToken();
        Product p = product(5, "10000");
        long listed = orderOf(token, p, 1);
        backdate(listed, 2);
        send(get("/api/orders"), token, null).andExpect(jsonPath("$.content[0].status").value("CANCELLED"));

        long scheduled = orderOf(token, p, 1);
        backdate(scheduled, 2);
        job.run();
        assertThat(orders().findById(scheduled).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stock(p)).isEqualTo(5);
    }

    @Autowired OrderRepository orderRepo;
    private OrderRepository orders() { return orderRepo; }

    @Test
    void payingAnExpiredOrderIs409AndOrderIsCancelled() throws Exception {
        String token = userToken();
        Product p = product(5, "10000");
        long id = orderOf(token, p, 2);
        backdate(id, 2);

        send(post("/api/orders/" + id + "/pay"), token, null).andExpect(status().isConflict());

        assertThat(orders().findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stock(p)).isEqualTo(5);
    }

    @Test
    void checkoutReleasesStockHeldByStaleOrders() throws Exception {
        Product p = product(2, "10000");
        String waiting = userToken();
        addToCart(waiting, p.getId(), 2);     // sudah di keranjang saat stok masih ada

        String first = userToken();
        long stale = orderOf(first, p, 2);    // order lain menahan seluruh stok
        assertThat(stock(p)).isZero();
        send(post("/api/orders"), waiting, SHIPPING).andExpect(status().isConflict());

        backdate(stale, 2);
        send(post("/api/orders"), waiting, SHIPPING).andExpect(status().isCreated()); // pengecekan malas membebaskan stok
        assertThat(stock(p)).isZero();
        assertThat(statusOf(first, stale)).isEqualTo("CANCELLED");
    }

    @Test
    void concurrentCancelAndExpiryRestockOnlyOnce() throws Exception {
        String token = userToken();
        Product p = product(5, "10000");
        long id = orderOf(token, p, 2);
        backdate(id, 2);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Future<Integer>> f = new ArrayList<>();
        f.add(pool.submit(() -> { barrier.await(); return send(post("/api/orders/" + id + "/cancel"), token, null).andReturn().getResponse().getStatus(); }));
        f.add(pool.submit(() -> { barrier.await(); return orderService.expireOverdue(); }));
        for (Future<Integer> x : f) x.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(stock(p)).isEqualTo(5); // 3 + 2, bukan 7
        assertThat(orders().findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
