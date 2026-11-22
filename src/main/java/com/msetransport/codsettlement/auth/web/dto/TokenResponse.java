package com.benzaazaa.codsettlement.auth.web.dto;

/** OAuth2-style token response. {@code expiresIn} is the access token lifetime in seconds. */
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
