package com.tokoku.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import static org.springframework.security.authorization.AuthorizationManagers.allOf;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasAuthority;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.tokoku.auth.Realm;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JsonMapper json) throws Exception {
        AuthenticationEntryPoint unauthorized = (req, res, ex) ->
                writeProblem(json, res, HttpStatus.UNAUTHORIZED, "Authentication required");
        AccessDeniedHandler forbidden = (req, res, ex) ->
                writeProblem(json, res, HttpStatus.FORBIDDEN, "You do not have permission");

        http
            .csrf(AbstractHttpConfigurer::disable) // API stateless + token di header, bukan cookie
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/api/payments/webhook", "/api/dev/**").permitAll() // webhook: signature dicek di service
                // Realm: token toko (REALM_store) tidak bisa ke admin, token admin (REALM_admin) tidak bisa ke endpoint pembeli
                .requestMatchers("/api/admin/auth/login", "/api/admin/auth/refresh", "/api/admin/auth/logout").permitAll()
                .requestMatchers("/api/admin/**").access(allOf(hasRole("ADMIN"), hasAuthority(Realm.ADMIN.authority())))
                .requestMatchers("/api/products/**", "/api/categories/**").access(allOf(hasRole("ADMIN"), hasAuthority(Realm.ADMIN.authority()))) // sisa method = tulis
                .requestMatchers("/api/me", "/api/cart/**", "/api/orders/**", "/api/auth/change-password").hasAuthority(Realm.STORE.authority())
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
            .oauth2ResourceServer(o -> o
                .jwt(j -> j.jwtAuthenticationConverter(jwtConverter()))
                .authenticationEntryPoint(unauthorized)
                .accessDeniedHandler(forbidden));
        return http.build();
    }

    // Klaim "role" menjadi ROLE_USER / ROLE_ADMIN; klaim "realm" menjadi REALM_store / REALM_admin
    private JwtAuthenticationConverter jwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> list = new ArrayList<>();
            String role = jwt.getClaimAsString("role");
            String realm = jwt.getClaimAsString("realm");
            if (role != null) list.add(new SimpleGrantedAuthority("ROLE_" + role));
            if (realm != null) list.add(new SimpleGrantedAuthority("REALM_" + realm));
            return list;
        });
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins:}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> list = Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        config.setAllowedOrigins(list);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static void writeProblem(JsonMapper json, jakarta.servlet.http.HttpServletResponse res, HttpStatus status,
            String detail) throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.getWriter().write(json.writeValueAsString(Map.of(
                "type", "about:blank", "title", status.getReasonPhrase(), "status", status.value(), "detail", detail)));
    }
}
