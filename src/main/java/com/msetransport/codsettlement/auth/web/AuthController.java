package com.benzaazaa.codsettlement.auth.web;

import com.benzaazaa.codsettlement.auth.service.AuthService;
import com.benzaazaa.codsettlement.auth.web.dto.LoginRequest;
import com.benzaazaa.codsettlement.auth.web.dto.MeResponse;
import com.benzaazaa.codsettlement.auth.web.dto.RefreshRequest;
import com.benzaazaa.codsettlement.auth.web.dto.TokenResponse;
import com.benzaazaa.codsettlement.security.AuthenticatedUser;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Auth", description = "Login, token refresh and current user")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return MeResponse.of(user);
    }
}
