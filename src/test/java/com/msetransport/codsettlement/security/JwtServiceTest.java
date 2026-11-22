package com.msetransport.codsettlement.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msetransport.codsettlement.account.domain.Role;
import com.msetransport.codsettlement.support.MutableClock;
import com.msetransport.codsettlement.support.TestData;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-characters";

    private final MutableClock clock = new MutableClock(TestData.NOW);
    private final JwtService jwt = new JwtService(
            new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(7), "cod-settlement"), clock);

    private final AuthenticatedUser merchantUser = new AuthenticatedUser(2L, "atlas", Role.MERCHANT, 1L);

    @Test
    void accessTokenRoundTripsThePrincipal() {
        assertThat(jwt.parseAccessToken(jwt.issueAccessToken(merchantUser))).isEqualTo(merchantUser);
    }

    @Test
    void staffTokenHasNoMerchant() {
        AuthenticatedUser staff = new AuthenticatedUser(1L, "ops", Role.STAFF, null);

        assertThat(jwt.parseAccessToken(jwt.issueAccessToken(staff)).merchantId()).isNull();
    }

    @Test
    void expiredTokenIsRejected() {
        String token = jwt.issueAccessToken(merchantUser);
        clock.advance(Duration.ofMinutes(16));

        assertThatThrownBy(() -> jwt.parseAccessToken(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token has expired");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwt.issueAccessToken(merchantUser);
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");

        assertThatThrownBy(() -> jwt.parseAccessToken(tampered)).hasMessage("Token is invalid");
        assertThatThrownBy(() -> jwt.parseAccessToken("not-a-jwt")).hasMessage("Token is invalid");
    }

    @Test
    void tokenSignedWithAnotherKeyOrIssuerIsRejected() {
        JwtService otherKey = new JwtService(new JwtProperties("another-secret-that-is-at-least-32-characters",
                Duration.ofMinutes(15), Duration.ofDays(7), "cod-settlement"), clock);
        JwtService otherIssuer = new JwtService(new JwtProperties(SECRET,
                Duration.ofMinutes(15), Duration.ofDays(7), "someone-else"), clock);

        assertThatThrownBy(() -> jwt.parseAccessToken(otherKey.issueAccessToken(merchantUser)))
                .isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> jwt.parseAccessToken(otherIssuer.issueAccessToken(merchantUser)))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refreshAndAccessTokensAreNotInterchangeable() {
        String refresh = jwt.issueRefreshToken(merchantUser);
        String access = jwt.issueAccessToken(merchantUser);

        assertThat(jwt.parseRefreshToken(refresh)).isEqualTo(2L);
        assertThatThrownBy(() -> jwt.parseAccessToken(refresh)).hasMessage("Wrong token type");
        assertThatThrownBy(() -> jwt.parseRefreshToken(access)).hasMessage("Wrong token type");
    }

    @Test
    void refreshTokenOutlivesTheAccessToken() {
        String refresh = jwt.issueRefreshToken(merchantUser);
        clock.advance(Duration.ofDays(6));

        assertThat(jwt.parseRefreshToken(refresh)).isEqualTo(2L);
        assertThat(jwt.accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
    }
}
