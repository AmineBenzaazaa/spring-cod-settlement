package com.msetransport.codsettlement.security;

import com.msetransport.codsettlement.account.domain.Role;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * The principal stored in the SecurityContext for every authenticated request. It is
 * rebuilt from the JWT claims, so no database query is needed per request.
 * Controllers receive it with {@code @AuthenticationPrincipal}.
 */
public record AuthenticatedUser(Long id, String username, Role role, Long merchantId) {

    public AuthenticatedUser {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(role, "role");
        if (role == Role.MERCHANT && merchantId == null) {
            throw new IllegalArgumentException("A merchant user must have a merchantId");
        }
    }

    public boolean isStaff() {
        return role == Role.STAFF;
    }

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }
}
