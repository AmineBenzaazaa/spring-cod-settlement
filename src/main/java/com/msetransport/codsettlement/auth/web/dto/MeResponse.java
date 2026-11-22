package com.msetransport.codsettlement.auth.web.dto;

import com.msetransport.codsettlement.account.domain.Role;
import com.msetransport.codsettlement.security.AuthenticatedUser;

public record MeResponse(Long id, String username, Role role, Long merchantId) {

    public static MeResponse of(AuthenticatedUser user) {
        return new MeResponse(user.id(), user.username(), user.role(), user.merchantId());
    }
}
