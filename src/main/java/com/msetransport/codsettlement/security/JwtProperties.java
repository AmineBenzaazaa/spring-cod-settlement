package com.msetransport.codsettlement.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration bound from {@code app.jwt.*}. {@code @Validated} makes the app refuse
 * to start with a missing or short secret instead of failing on the first login.
 *
 * @param secret HMAC-SHA256 key; at least 32 bytes (256 bits) as required by RFC 7518
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "must be at least 32 characters (set APP_JWT_SECRET)") String secret,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl,
        @NotBlank String issuer) {
}
