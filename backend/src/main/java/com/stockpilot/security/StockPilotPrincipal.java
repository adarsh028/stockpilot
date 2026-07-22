package com.stockpilot.security;

import com.stockpilot.user.UserRole;

import java.util.UUID;

/**
 * The authenticated caller derived from the JWT. organizationId here is the
 * single source of tenant identity for the whole request — never taken from
 * the request body or path.
 */
public record StockPilotPrincipal(
        UUID userId,
        UUID organizationId,
        UserRole role,
        String email
) {
}
