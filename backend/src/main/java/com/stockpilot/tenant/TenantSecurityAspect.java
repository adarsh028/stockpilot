package com.stockpilot.tenant;

import com.stockpilot.common.entity.TenantEntity;
import com.stockpilot.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Last-resort net: after any *Service method returns a TenantEntity (or a collection
 * of them), verify every returned entity belongs to the current tenant. Catches any
 * finder that forgot to scope by organizationId. Skips when there is no authenticated
 * tenant (e.g. the unauthenticated auth/signup flow).
 */
@Aspect
@Component
@RequiredArgsConstructor
public class TenantSecurityAspect {

    private final CurrentTenant currentTenant;

    @AfterReturning(pointcut = "execution(* com.stockpilot..*Service.*(..))", returning = "result")
    public void verifyTenantOwnership(Object result) {
        if (result == null) {
            return;
        }
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return;
        }
        if (result instanceof TenantEntity entity) {
            check(entity);
        } else if (result instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item instanceof TenantEntity entity) {
                    check(entity);
                }
            }
        }
    }

    private void check(TenantEntity entity) {
        if (!currentTenant.organizationId().equals(entity.getOrganizationId())) {
            throw new ResourceNotFoundException("Resource not found");
        }
    }
}
