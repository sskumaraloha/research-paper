package com.store.app.security;

import com.store.app.user.entity.RoleName;
import com.store.app.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapts the {@link User} entity to Spring Security's {@link UserDetails}.
 * The username is the user's phone number.
 */
public class StoreUserDetails implements UserDetails {

    @Getter
    private final User user;
    private final List<GrantedAuthority> authorities;

    public StoreUserDetails(User user) {
        this.user = user;
        this.authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role.getName().name()))
                .toList();
    }

    /**
     * Customer accounts must verify their phone before logging in.
     * Admin accounts are exempt (they are provisioned, not self-registered).
     */
    public boolean requiresPhoneVerification() {
        return !user.isPhoneVerified()
                && hasRole(RoleName.ROLE_CUSTOMER)
                && !hasRole(RoleName.ROLE_ADMIN);
    }

    private boolean hasRole(RoleName roleName) {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals(roleName.name()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getPhoneNumber();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
