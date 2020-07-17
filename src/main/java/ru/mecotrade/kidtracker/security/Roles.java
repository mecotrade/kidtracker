package ru.mecotrade.kidtracker.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum Roles {

    USER,
    ADMIN;

    private final GrantedAuthority authority;

    private Roles() {
        authority = new SimpleGrantedAuthority(this.name());
    }

    public GrantedAuthority toAuthority() {
        return authority;
    }
}
