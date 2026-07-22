package com.stockpilot.tenant;

import com.stockpilot.security.SecurityUtils;
import com.stockpilot.user.UserRole;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

/**
 * Request-scoped accessor for the current tenant. Injected into every service so
 * organizationId always comes from the JWT — never from client-supplied input.
 */
@Component
@RequestScope
public class CurrentTenant {

    public UUID organizationId() {
        return SecurityUtils.currentPrincipal().organizationId();
    }

    public UUID userId() {
        return SecurityUtils.currentPrincipal().userId();
    }

    public UserRole role() {
        return SecurityUtils.currentPrincipal().role();
    }
}
