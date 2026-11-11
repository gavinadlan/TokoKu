package com.tokoku.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.TestcontainersConfiguration;
import com.tokoku.auth.Role;
import com.tokoku.auth.User;
import com.tokoku.auth.UserRepository;
import com.tokoku.product.Category;
import com.tokoku.product.CategoryRepository;
import com.tokoku.product.Product;
import com.tokoku.product.ProductRepository;

// Dasar test integrasi: satu context + satu container Postgres dipakai bersama semua subclass
@SpringBootTest(properties = { "app.payment.provider=fake", "app.images.provider=fake", "app.mail.enabled=false", "app.payment.reconcile.initial-delay-ms=3600000", "app.ratelimit.login.per-email=50", "app.ratelimit.login.per-ip=100000",
        "app.ratelimit.admin-login.per-email=1000", "app.ratelimit.admin-login.per-ip=100000" })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class ApiTestBase {

    protected static final String PASSWORD = "password123";

    @Autowired protected MockMvc mvc;
    @Autowired protected UserRepository users;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected CategoryRepository categories;
    @Autowired protected ProductRepository products;

    protected String uniqueEmail() {
        return "u" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    protected String json(String url, String token, String body) throws Exception {
        var req = post(url).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req).andReturn().getResponse().getContentAsString();
    }

    protected String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    /** Mendaftar customer baru lewat API lalu login; mengembalikan access token. */
    protected String userToken() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\",\"name\":\"Tester\"}".formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return login(email, PASSWORD);
    }

    /** Membuat admin langsung di DB (register selalu USER) lalu login lewat endpoint admin. */
    protected String adminToken() throws Exception {
        String email = uniqueEmail();
        users.save(new User(email, encoder.encode(PASSWORD), "Admin", Role.ADMIN));
        return adminLogin(email, PASSWORD);
    }

    protected String adminLogin(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    /** Produk baru langsung di DB (kategori unik) supaya tiap test punya data sendiri. */
    protected Product product(int stock, String price) {
        Category c = categories.save(new Category("cat-" + UUID.randomUUID()));
        return products.save(new Product(c, "Item " + UUID.randomUUID().toString().substring(0, 6), "desc",
                new java.math.BigDecimal(price), stock));
    }

    protected org.springframework.test.web.servlet.ResultActions send(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req, String token, String body) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(req);
    }

    protected static final String SHIPPING =
            "{\"shippingName\":\"Budi\",\"shippingPhone\":\"08123456789\",\"shippingAddress\":\"Jl. Merdeka 1, Jakarta\"}";

    protected void addToCart(String token, long productId, int qty) throws Exception {
        send(post("/api/cart/items"), token, "{\"productId\":%d,\"quantity\":%d}".formatted(productId, qty))
                .andExpect(status().isOk());
    }

    /** Checkout dan kembalikan id order. */
    protected long checkout(String token) throws Exception {
        String body = send(post("/api/orders"), token, SHIPPING).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
