package com.msetransport.codsettlement.auth.web.dto;

import com.msetransport.codsettlement.account.domain.Role;
import com.msetransport.codsettlement.security.AuthenticatedUser;

/** The signed-in user. {@code merchantName} is the merchant's display name (null for staff), for the UI. */
public record MeResponse(Long id, String username, Role role, Long merchantId, String merchantName) {

    public static MeResponse of(AuthenticatedUser user, String merchantName) {
        return new MeResponse(user.id(), user.username(), user.role(), user.merchantId(), merchantName);
    }
}
