package com.brimmatech.apps.deapi.security;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@NoArgsConstructor(force = true)
@Data
public class ApiKeyAuthenticationToken implements Authentication {

    @Builder
    public record Credentials(String apiKey, Long tenantId){};

    private final Credentials credentials;
    private boolean isAuthenticated;
    private Collection<? extends GrantedAuthority> authorities = List.of();
    private Long dummyPrincipal;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return dummyPrincipal;
    }

    @Override
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.isAuthenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return "";
    }
}
