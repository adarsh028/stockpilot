package com.stockpilot.organization;

import com.stockpilot.common.exception.ResourceNotFoundException;
import com.stockpilot.organization.dto.OrganizationResponse;
import com.stockpilot.organization.dto.UpdateOrganizationRequest;
import com.stockpilot.tenant.CurrentTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final CurrentTenant currentTenant;

    @Transactional(readOnly = true)
    public OrganizationResponse getCurrent() {
        return toResponse(load());
    }

    @Transactional
    public OrganizationResponse update(UpdateOrganizationRequest req) {
        Organization org = load();
        org.setName(req.name());
        return toResponse(organizationRepository.save(org));
    }

    private Organization load() {
        return organizationRepository.findById(currentTenant.organizationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Organization", currentTenant.organizationId()));
    }

    private OrganizationResponse toResponse(Organization org) {
        return new OrganizationResponse(
                org.getId().toString(),
                org.getName(),
                org.getSlug(),
                org.getPlan().name(),
                org.getStatus().name(),
                org.getCreatedAt()
        );
    }
}
