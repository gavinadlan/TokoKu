package com.tokoku.common;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tokoku.product.Category;
import com.tokoku.product.CategoryRepository;
import com.tokoku.product.Product;
import com.tokoku.product.ProductRepository;

/** Data contoh bertema kopi dan perlengkapan seduh. Hanya profil dev; dilewati jika sudah ada produk. */
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private record Item(String name, String description, String price, int stock) {}

    private final CategoryRepository categories;
    private final ProductRepository products;

    public DevDataSeeder(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (products.count() > 0) {
            return; // idempoten
        }
        seed("Biji Kopi Single Origin", List.of(
                new Item("Kopi Arabika Gayo 250g", "Medium roast, aroma rempah dan cokelat, asal Aceh Tengah.", "85000", 40),
                new Item("Kopi Arabika Toraja Sapan 250g", "Body tebal, after-taste manis karamel.", "92000", 35),
                new Item("Kopi Arabika Flores Bajawa 250g", "Light-medium roast, nuansa kacang dan jeruk.", "88000", 30),
                new Item("Kopi Arabika Kintamani 250g", "Keasaman cerah dengan rasa buah sitrus.", "86000", 30),
                new Item("Kopi Robusta Temanggung 250g", "Pahit pekat, crema tebal, cocok untuk kopi susu.", "62000", 50)));
        seed("Biji Kopi Blend", List.of(
                new Item("House Blend Espresso 1kg", "Campuran arabika dan robusta untuk mesin espresso.", "215000", 25),
                new Item("Blend Kopi Susu 500g", "Roasting medium-dark, manis dan creamy bersama susu.", "98000", 45),
                new Item("Blend Tubruk Klasik 250g", "Gilingan kasar untuk seduh tubruk ala warung kopi.", "48000", 60),
                new Item("Decaf Arabika 250g", "Kopi rendah kafein, rasa halus dan bersih.", "105000", 20),
                new Item("Cold Brew Blend 500g", "Dirancang untuk seduh dingin 12 jam, rasa cokelat ringan.", "110000", 25)));
        seed("Alat Seduh Manual", List.of(
                new Item("Dripper V60 Keramik 02", "Dripper kerucut dengan spiral rib untuk aliran seragam.", "165000", 30),
                new Item("French Press 600ml", "Gelas borosilikat dan rangka stainless steel.", "185000", 25),
                new Item("Moka Pot 6 Cup Aluminium", "Seduh stovetop ala Italia, cocok untuk kompor gas.", "145000", 25),
                new Item("AeroPress Original", "Seduh cepat 2 menit, mudah dibawa bepergian.", "525000", 15),
                new Item("Teko Leher Angsa 600ml", "Teko stainless dengan kontrol tuang presisi.", "275000", 20)));
        seed("Grinder dan Timbangan", List.of(
                new Item("Grinder Manual Burr Keramik", "Burr keramik, pengaturan gilingan bertingkat.", "395000", 20),
                new Item("Grinder Manual Stainless Pro", "Burr baja, poros bearing ganda untuk giling halus.", "820000", 10),
                new Item("Grinder Listrik Burr Datar", "Kapasitas hopper 250g, 30 tingkat gilingan.", "1450000", 8),
                new Item("Timbangan Kopi Digital 0.1g", "Timer bawaan, kapasitas 2kg, layar mudah dibaca.", "325000", 25),
                new Item("Termometer Teko Digital", "Ukur suhu air seduh dari 0 hingga 100 derajat Celsius.", "95000", 30)));
        seed("Gelas dan Aksesori", List.of(
                new Item("Mug Keramik Matte 300ml", "Mug tebal menahan panas, tersedia warna hitam doff.", "55000", 50),
                new Item("Gelas Cortado 150ml Double Wall", "Gelas kaca dinding ganda untuk espresso dan cortado.", "78000", 40),
                new Item("Tumbler Stainless 450ml", "Menjaga suhu panas 6 jam dan dingin 12 jam.", "135000", 35),
                new Item("Tamper Stainless 51mm", "Tamper datar dengan gagang kayu jati.", "125000", 20),
                new Item("Toples Kedap Udara 500g", "Penyimpan biji kopi dengan katup satu arah.", "89000", 40)));
        seed("Filter dan Perlengkapan", List.of(
                new Item("Kertas Filter V60 02 isi 100", "Kertas filter putih tanpa bau kertas.", "42000", 80),
                new Item("Kertas Filter AeroPress isi 350", "Pas untuk AeroPress original dan XL.", "65000", 60),
                new Item("Filter Kain Reusable", "Filter kain katun untuk seduh tubruk lebih bersih.", "38000", 40),
                new Item("Sikat Pembersih Grinder", "Sikat halus untuk membersihkan sisa bubuk di burr.", "28000", 50),
                new Item("Descaler Mesin Kopi 250ml", "Pembersih kerak untuk mesin espresso dan teko listrik.", "72000", 30)));
        log.info("Dev seed data created: {} categories, {} products", categories.count(), products.count());
    }

    private void seed(String categoryName, List<Item> items) {
        Category c = categories.save(new Category(categoryName));
        for (Item i : items) {
            products.save(new Product(c, i.name(), i.description(), new BigDecimal(i.price()), i.stock()));
        }
    }
}
