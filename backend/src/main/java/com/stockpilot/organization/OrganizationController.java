package com.stockpilot.organization;

import com.stockpilot.organization.dto.OrganizationResponse;
import com.stockpilot.organization.dto.UpdateOrganizationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organization")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public OrganizationResponse current() {
        return organizationService.getCurrent();
    }

    @PatchMapping
    @PreAuthorize("hasRole('OWNER')")
    public OrganizationResponse update(@Valid @RequestBody UpdateOrganizationRequest req) {
        return organizationService.update(req);
    }
}
