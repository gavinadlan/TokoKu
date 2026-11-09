package com.tokoku.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

import com.tokoku.support.ApiTestBase;

class OpenApiIntegrationTest extends ApiTestBase {

    @Test
    void docsExposeBearerSchemeAndEveryModuleEndpoint() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").isEmpty()) // publik: tanpa gembok
                .andExpect(jsonPath("$.security[0].bearerAuth").exists()) // default global: butuh token
                .andExpect(jsonPath("$.paths['/api/cart'].get.security").doesNotExist()) // mewarisi default
                .andExpect(jsonPath("$.paths['/api/products'].get").exists())
                .andExpect(jsonPath("$.paths['/api/orders/{id}/pay'].post").exists())
                .andExpect(jsonPath("$.paths['/api/payments/webhook'].post").exists())
                .andExpect(jsonPath("$.paths['/api/admin/orders'].get").exists())
                .andExpect(jsonPath("$.paths['/api/admin/products'].get").exists())
                .andExpect(jsonPath("$.paths['/api/me'].get").exists());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }
}
