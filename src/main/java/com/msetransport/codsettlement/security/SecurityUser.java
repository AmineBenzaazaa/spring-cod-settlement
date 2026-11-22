package com.msetransport.codsettlement.security;

import com.msetransport.codsettlement.account.domain.AppUser;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adapter between our {@link AppUser} entity and Spring Security's {@link UserDetails},
 * used only during username/password login. Copies the fields it needs so it never
 * touches a lazy association outside a transaction.
 */
public class SecurityUser implements UserDetails {

    private final AuthenticatedUser user;
    private final String passwordHash;
    private final boolean enabled;

    public SecurityUser(AppUser appUser) {
        this.user = new AuthenticatedUser(appUser.getId(), appUser.getUsername(), appUser.getRole(),
                appUser.getMerchantId());
        this.passwordHash = appUser.getPasswordHash();
        this.enabled = appUser.isEnabled();
    }

    public AuthenticatedUser toAuthenticatedUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.authorities();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return user.username();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
