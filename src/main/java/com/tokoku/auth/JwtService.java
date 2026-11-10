package com.tokoku.auth;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

// Menerbitkan access token. Verifikasi token dilakukan Spring Security (JwtDecoder di JwtConfig).
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final Duration storeTtl;
    private final Duration adminTtl;

    public JwtService(JwtEncoder encoder, @Value("${app.jwt.expiration-minutes:30}") long expirationMinutes,
            @Value("${app.jwt.admin-expiration-minutes:15}") long adminExpirationMinutes) {
        this.encoder = encoder;
        this.storeTtl = Duration.ofMinutes(expirationMinutes);
        this.adminTtl = Duration.ofMinutes(adminExpirationMinutes);
    }

    public String issue(User user, Realm realm) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("tokoku")
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(ttl(realm)))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("realm", realm.claim())
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private Duration ttl(Realm realm) { return realm == Realm.ADMIN ? adminTtl : storeTtl; }

    public long ttlSeconds(Realm realm) { return ttl(realm).toSeconds(); }
}
