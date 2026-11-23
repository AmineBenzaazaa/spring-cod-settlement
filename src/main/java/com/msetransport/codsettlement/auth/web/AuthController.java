package com.msetransport.codsettlement.auth.web;

import com.msetransport.codsettlement.account.domain.Merchant;
import com.msetransport.codsettlement.account.repository.MerchantRepository;
import com.msetransport.codsettlement.auth.service.AuthService;
import com.msetransport.codsettlement.auth.web.dto.LoginRequest;
import com.msetransport.codsettlement.auth.web.dto.MeResponse;
import com.msetransport.codsettlement.auth.web.dto.RefreshRequest;
import com.msetransport.codsettlement.auth.web.dto.TokenResponse;
import com.msetransport.codsettlement.security.AuthenticatedUser;
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
    private final MerchantRepository merchants;

    public AuthController(AuthService authService, MerchantRepository merchants) {
        this.authService = authService;
        this.merchants = merchants;
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
        String merchantName = user.merchantId() == null ? null
                : merchants.findById(user.merchantId()).map(Merchant::getName).orElse(null);
        return MeResponse.of(user, merchantName);
    }
}
