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

// Dasar test integrasi: satu context + satu container Postgres dipakai bersama semua subclass
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class ApiTestBase {

    protected static final String PASSWORD = "password123";

    @Autowired protected MockMvc mvc;
    @Autowired protected UserRepository users;
    @Autowired protected PasswordEncoder encoder;

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

    /** Membuat admin langsung di DB (register selalu USER) lalu login. */
    protected String adminToken() throws Exception {
        String email = uniqueEmail();
        users.save(new User(email, encoder.encode(PASSWORD), "Admin", Role.ADMIN));
        return login(email, PASSWORD);
    }
}
