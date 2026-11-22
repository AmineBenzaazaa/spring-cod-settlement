package com.msetransport.codsettlement.security;

import com.msetransport.codsettlement.account.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies HS256-signed JWTs.
 *
 * <ul>
 *   <li><b>Access token</b> (short-lived, 15 min by default): carries user id, role and
 *       merchant id, so each request is authorized without a database lookup.</li>
 *   <li><b>Refresh token</b> (long-lived): only carries the user id. Exchanging it reloads
 *       the user from the database, so a disabled user cannot refresh.</li>
 * </ul>
 *
 * The {@code typ} claim stops a refresh token from being used as an access token.
 */
@Service
public class JwtService {

    static final String CLAIM_TYPE = "typ";
    static final String CLAIM_USER_ID = "uid";
    static final String CLAIM_ROLE = "role";
    static final String CLAIM_MERCHANT_ID = "mid";
    static final String TYPE_ACCESS = "access";
    static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey key;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(AuthenticatedUser user) {
        return Jwts.builder()
                .subject(user.username())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_USER_ID, user.id())
                .claim(CLAIM_ROLE, user.role().name())
                .claim(CLAIM_MERCHANT_ID, user.merchantId()) // null claims are omitted
                .issuer(properties.issuer())
                .issuedAt(Date.from(now()))
                .expiration(Date.from(now().plus(properties.accessTokenTtl())))
                .signWith(key)
                .compact();
    }

    public String issueRefreshToken(AuthenticatedUser user) {
        return Jwts.builder()
                .subject(user.username())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim(CLAIM_USER_ID, user.id())
                .issuer(properties.issuer())
                .issuedAt(Date.from(now()))
                .expiration(Date.from(now().plus(properties.refreshTokenTtl())))
                .signWith(key)
                .compact();
    }

    /** Verifies signature, issuer, expiry and type, then rebuilds the principal from the claims. */
    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = verify(token, TYPE_ACCESS);
        try {
            Number merchantId = claims.get(CLAIM_MERCHANT_ID, Number.class);
            return new AuthenticatedUser(
                    claims.get(CLAIM_USER_ID, Number.class).longValue(),
                    claims.getSubject(),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                    merchantId == null ? null : merchantId.longValue());
        } catch (RuntimeException ex) {
            throw new InvalidTokenException("Token claims are invalid");
        }
    }

    /** @return the user id the refresh token was issued to */
    public long parseRefreshToken(String token) {
        Claims claims = verify(token, TYPE_REFRESH);
        Number userId = claims.get(CLAIM_USER_ID, Number.class);
        if (userId == null) {
            throw new InvalidTokenException("Token claims are invalid");
        }
        return userId.longValue();
    }

    public Duration accessTokenTtl() {
        return properties.accessTokenTtl();
    }

    private Claims verify(String token, String expectedType) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .clock(() -> Date.from(now()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new InvalidTokenException("Token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Token is invalid");
        }
        if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new InvalidTokenException("Wrong token type");
        }
        return claims;
    }

    private Instant now() {
        return clock.instant();
    }
}
