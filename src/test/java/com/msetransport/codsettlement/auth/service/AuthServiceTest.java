package com.msetransport.codsettlement.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.msetransport.codsettlement.account.domain.AppUser;
import com.msetransport.codsettlement.account.repository.AppUserRepository;
import com.msetransport.codsettlement.auth.web.dto.LoginRequest;
import com.msetransport.codsettlement.auth.web.dto.TokenResponse;
import com.msetransport.codsettlement.security.InvalidTokenException;
import com.msetransport.codsettlement.security.JwtProperties;
import com.msetransport.codsettlement.security.JwtService;
import com.msetransport.codsettlement.security.SecurityUser;
import com.msetransport.codsettlement.support.TestData;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock AppUserRepository users;

    private JwtService jwt;
    private AuthService service;
    private AppUser atlas;

    @BeforeEach
    void setUp() {
        jwt = new JwtService(new JwtProperties("unit-test-secret-that-is-at-least-32-characters",
                Duration.ofMinutes(15), Duration.ofDays(7), "cod-settlement"), Clock.fixed(TestData.NOW, ZoneOffset.UTC));
        service = new AuthService(authenticationManager, users, jwt);
        atlas = TestData.appUser(2, TestData.merchant(1));
    }

    @Test
    void loginReturnsAnAccessAndRefreshTokenPair() {
        SecurityUser principal = new SecurityUser(atlas);
        when(authenticationManager.authenticate(any()))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

        TokenResponse tokens = service.login(new LoginRequest("user2", "pw"));

        assertThat(tokens.tokenType()).isEqualTo("Bearer");
        assertThat(tokens.expiresIn()).isEqualTo(900);
        assertThat(jwt.parseAccessToken(tokens.accessToken()).merchantId()).isEqualTo(1L);
        assertThat(jwt.parseRefreshToken(tokens.refreshToken())).isEqualTo(2L);
    }

    @Test
    void everyLoginFailureGetsTheSameMessage() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> service.login(new LoginRequest("user2", "pw")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password.");
    }

    @Test
    void refreshReloadsTheUserAndIssuesNewTokens() {
        when(users.findById(2L)).thenReturn(Optional.of(atlas));

        TokenResponse tokens = service.refresh(jwt.issueRefreshToken(new SecurityUser(atlas).toAuthenticatedUser()));

        assertThat(jwt.parseAccessToken(tokens.accessToken()).username()).isEqualTo("user2");
    }

    @Test
    void disabledUserCannotRefresh() {
        atlas.disable();
        when(users.findById(2L)).thenReturn(Optional.of(atlas));
        String refresh = jwt.issueRefreshToken(new SecurityUser(atlas).toAuthenticatedUser());

        assertThatThrownBy(() -> service.refresh(refresh)).isInstanceOf(InvalidTokenException.class);
    }
}
