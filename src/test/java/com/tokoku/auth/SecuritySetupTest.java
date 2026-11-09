package com.tokoku.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.tokoku.common.JwtConfig;

class SecuritySetupTest {

    @Test
    void prodWithoutJwtSecretFailsToStart() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        assertThatThrownBy(() -> new JwtConfig("", env)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shortJwtSecretIsRejected() {
        assertThatThrownBy(() -> new JwtConfig("too-short", new MockEnvironment())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void adminBootstrapCreatesAdminOnlyWhenNoneExists() {
        UserRepository repo = mock(UserRepository.class);
        var bootstrap = new AdminBootstrap(repo, new BCryptPasswordEncoder(), "Boss@Example.com", "some-password-1");

        when(repo.existsByRole(Role.ADMIN)).thenReturn(false);
        bootstrap.run(null);
        verify(repo, times(1)).save(any(User.class));

        when(repo.existsByRole(Role.ADMIN)).thenReturn(true);
        bootstrap.run(null);
        verify(repo, times(1)).save(any(User.class)); // tidak bertambah: idempoten
    }

    @Test
    void adminBootstrapSkippedWhenEnvEmpty() {
        UserRepository repo = mock(UserRepository.class);
        new AdminBootstrap(repo, new BCryptPasswordEncoder(), "", "").run(null);
        verifyNoInteractions(repo);
    }
}
