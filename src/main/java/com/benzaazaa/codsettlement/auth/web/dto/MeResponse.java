package com.benzaazaa.codsettlement.auth.web.dto;

import com.benzaazaa.codsettlement.account.domain.Role;
import com.benzaazaa.codsettlement.security.AuthenticatedUser;

public record MeResponse(Long id, String username, Role role, Long merchantId) {

    public static MeResponse of(AuthenticatedUser user) {
        return new MeResponse(user.id(), user.username(), user.role(), user.merchantId());
    }
}
