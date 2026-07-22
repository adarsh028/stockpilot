package com.stockpilot.security;

import com.stockpilot.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static StockPilotPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof StockPilotAuthenticationToken token) {
            return token.getPrincipal();
        }
        throw new UnauthorizedException("No authenticated user in security context");
    }
}
