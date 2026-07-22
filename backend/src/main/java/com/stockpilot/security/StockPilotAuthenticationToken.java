package com.stockpilot.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class StockPilotAuthenticationToken extends AbstractAuthenticationToken {

    private final StockPilotPrincipal principal;

    public StockPilotAuthenticationToken(StockPilotPrincipal principal) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public StockPilotPrincipal getPrincipal() {
        return principal;
    }
}
