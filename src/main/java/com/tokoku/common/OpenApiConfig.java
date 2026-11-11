package com.tokoku.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.Map;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI tokokuOpenApi() {
        return new OpenAPI()
                .info(new Info().title("TokoKu API").version("1.0")
                        .description("Backend e-commerce TokoKu. Klik **Authorize** lalu tempel access token dari `POST /api/auth/login`."))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                // Default: semua endpoint butuh token; endpoint publik menimpa dengan @SecurityRequirements kosong
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .addTagsItem(new Tag().name("Auth").description("Registrasi, login, profil"))
                .addTagsItem(new Tag().name("Produk").description("Katalog produk (baca publik, tulis admin)"))
                .addTagsItem(new Tag().name("Kategori").description("Kategori produk"))
                .addTagsItem(new Tag().name("Keranjang").description("Keranjang belanja customer"))
                .addTagsItem(new Tag().name("Order").description("Checkout dan riwayat order customer"))
                .addTagsItem(new Tag().name("Pembayaran").description("Pembayaran Midtrans dan webhook"))
                .addTagsItem(new Tag().name("Toko").description("Info dan pengaturan toko"))
                .addTagsItem(new Tag().name("Admin").description("Khusus role ADMIN"))
                .addTagsItem(new Tag().name("Dev").description("Hanya dev/test dengan provider fake"));
    }

    // Record Java tidak menandai komponen sebagai required di OpenAPI, sehingga klien TypeScript yang dibangkitkan
    // menganggap semua field respons opsional. Di sini semua field respons ditandai required, kecuali yang memang boleh null.
    private static final Map<String, Set<String>> NULLABLE = Map.of(
            "ProductResponse", Set.of("description", "imageUrl"),
            "ProductImageResponse", Set.of("width", "height"),
            "CartItemResponse", Set.of("imageUrl"),
            "PayResponse", Set.of("clientKey"),
            "OrderResponse", Set.of("shippingAddress", "courier", "trackingNumber", "pickupCode", "pickupReadyAt", "invoiceNumber", "invoicedAt"));

    @Bean
    OpenApiCustomizer responseFieldsRequired() {
        return openApi -> openApi.getComponents().getSchemas().forEach((name, schema) -> {
            boolean response = name.endsWith("Response") && !name.equals("StoreSettingsRequest") || name.equals("PageMetadata") || name.equals("WebhookResult")
                    || name.equals("SimulationResult") || name.equals("UploadSignature") || name.startsWith("PagedModel");
            if (!response || schema.getProperties() == null) {
                return;
            }
            Set<String> nullable = NULLABLE.getOrDefault(name, Set.of());
            schema.setRequired(schema.getProperties().keySet().stream().filter(k -> !nullable.contains(k)).sorted().toList());
            nullable.forEach(k -> {
                io.swagger.v3.oas.models.media.Schema<?> prop = (io.swagger.v3.oas.models.media.Schema<?>) schema.getProperties().get(k);
                if (prop != null) {
                    prop.addType("null");
                }
            });
        });
    }
}
