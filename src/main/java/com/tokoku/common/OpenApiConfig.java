package com.tokoku.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

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
                .addTagsItem(new Tag().name("Admin").description("Khusus role ADMIN"))
                .addTagsItem(new Tag().name("Dev").description("Hanya dev/test dengan provider fake"));
    }
}
