package com.tokoku.common;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    private final SecretKey key;

    public JwtConfig(@Value("${app.jwt.secret:}") String secret, Environment env) {
        if (secret.isBlank()) {
            if (env.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("JWT_SECRET must be set (>= 32 bytes) in the prod profile");
            }
            // Dev/test: kunci acak per start, tidak ada secret di repo. Token lama tidak valid setelah restart.
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            this.key = new SecretKeySpec(random, "HmacSHA256");
            log.warn("JWT_SECRET not set: using an ephemeral key (tokens become invalid on restart)");
            return;
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
