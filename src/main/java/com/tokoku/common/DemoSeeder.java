package com.tokoku.common;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tokoku.auth.Role;
import com.tokoku.auth.User;
import com.tokoku.auth.UserRepository;
import com.tokoku.product.Category;
import com.tokoku.product.CategoryRepository;
import com.tokoku.product.Product;
import com.tokoku.product.ProductRepository;
import com.tokoku.store.StoreService;

/**
 * Data demo AMAN untuk produksi, hanya bila APP_SEED_DEMO=true. Idempoten dan tidak pernah menimpa data nyata:
 * produk hanya bila katalog kosong, identitas toko hanya bila belum diisi, akun pembeli demo hanya bila
 * DEMO_USER_PASSWORD diisi (tidak ada password bawaan di kode). Tidak membuat akun admin.
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true")
public class DemoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);

    private record Item(String name, String description, String price, int stock) {}

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final UserRepository users;
    private final StoreService store;
    private final PasswordEncoder encoder;
    private final String demoEmail;
    private final String demoPassword;

    public DemoSeeder(CategoryRepository categories, ProductRepository products, UserRepository users, StoreService store,
            PasswordEncoder encoder, @Value("${app.seed.demo-user-email:demo@tokoku.example}") String demoEmail,
            @Value("${app.seed.demo-user-password:}") String demoPassword) {
        this.categories = categories;
        this.products = products;
        this.users = users;
        this.store = store;
        this.encoder = encoder;
        this.demoEmail = demoEmail.trim().toLowerCase();
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (products.count() == 0) {
            seed("Biji Kopi", List.of(
                    new Item("Kopi Arabika Gayo 250g", "Medium roast, aroma rempah dan cokelat.", "85000", 40),
                    new Item("Kopi Robusta Temanggung 250g", "Pahit pekat, crema tebal.", "62000", 50),
                    new Item("House Blend Espresso 1kg", "Campuran arabika dan robusta untuk espresso.", "215000", 25)));
            seed("Alat Seduh", List.of(
                    new Item("Dripper V60 Keramik 02", "Dripper kerucut dengan spiral rib.", "165000", 30),
                    new Item("French Press 600ml", "Gelas borosilikat dan rangka stainless steel.", "185000", 25),
                    new Item("Timbangan Kopi Digital 0.1g", "Timer bawaan, kapasitas 2kg.", "325000", 25)));
        }
        var s = store.current();
        if (!s.isConfigured()) {
            s.update("TokoKu Demo", "Jl. Contoh No. 1, Kota Anda", "0800-0000-0000", "demo@tokoku.example",
                    "Senin sampai Sabtu, 09.00 sampai 17.00", "Tunjukkan kode ambil kepada petugas di kasir.",
                    true, true, new BigDecimal("15000"), new BigDecimal("500000"),
                    "Ini toko demo; pesanan tidak diproses.", null, null);
        }
        if (!demoPassword.isBlank() && !users.existsByEmail(demoEmail)) {
            users.save(new User(demoEmail, encoder.encode(demoPassword), "Pembeli Demo", Role.USER));
        }
        log.info("Demo seed selesai: {} produk", products.count());
    }

    private void seed(String categoryName, List<Item> items) {
        Category c = categories.save(new Category(categoryName));
        for (Item i : items) {
            products.save(new Product(c, i.name(), i.description(), new BigDecimal(i.price()), i.stock()));
        }
    }
}
