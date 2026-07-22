package com.stockpilot.tenant;

import com.stockpilot.common.entity.TenantEntity;
import com.stockpilot.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Belt-and-braces ownership check. Even though tenant repositories are expected to
 * filter by organizationId in the query itself, this re-verifies the loaded entity's
 * org before it is returned or mutated. Returns 404 (not 403) on mismatch so callers
 * cannot probe for the existence of another org's records.
 */
@Component
public class TenantGuard {

    public <T extends TenantEntity> T loadOwned(String entityName, Object id,
                                                UUID organizationId, Supplier<Optional<T>> finder) {
        T entity = finder.get().orElseThrow(() -> ResourceNotFoundException.of(entityName, id));
        if (!organizationId.equals(entity.getOrganizationId())) {
            throw ResourceNotFoundException.of(entityName, id);
        }
        return entity;
    }
}
