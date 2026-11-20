package com.benzaazaa.codsettlement.auth.service;

import com.benzaazaa.codsettlement.account.domain.AppUser;
import com.benzaazaa.codsettlement.account.repository.AppUserRepository;
import com.benzaazaa.codsettlement.auth.web.dto.LoginRequest;
import com.benzaazaa.codsettlement.auth.web.dto.TokenResponse;
import com.benzaazaa.codsettlement.security.AuthenticatedUser;
import com.benzaazaa.codsettlement.security.InvalidTokenException;
import com.benzaazaa.codsettlement.security.JwtService;
import com.benzaazaa.codsettlement.security.SecurityUser;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository users;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, AppUserRepository users, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.jwtService = jwtService;
    }

    public TokenResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            // One message for unknown user, wrong password and disabled account:
            // never tell an attacker which usernames exist.
            throw new BadCredentialsException("Invalid username or password.");
        }
        SecurityUser principal = (SecurityUser) authentication.getPrincipal();
        return issueTokens(principal.toAuthenticatedUser());
    }

    /** Exchanges a valid refresh token for a new token pair, re-reading the user from the database. */
    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        long userId = jwtService.parseRefreshToken(refreshToken);
        AppUser user = users.findById(userId)
                .filter(AppUser::isEnabled)
                .orElseThrow(() -> new InvalidTokenException("User no longer active"));
        return issueTokens(new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole(), user.getMerchantId()));
    }

    private TokenResponse issueTokens(AuthenticatedUser user) {
        return TokenResponse.bearer(
                jwtService.issueAccessToken(user),
                jwtService.issueRefreshToken(user),
                jwtService.accessTokenTtl().toSeconds());
    }
}
