package com.stockpilot.integration.drive;

import com.stockpilot.integration.drive.dto.AuthorizeUrlResponse;
import com.stockpilot.integration.drive.dto.DriveStatusResponse;
import com.stockpilot.tenant.CurrentTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Per-tenant Google Drive connection endpoints. {@code /callback} is public (Google
 * redirects the browser to it without a JWT) and is secured instead by the signed
 * {@code state} parameter; everything else is authenticated and owner/admin gated.
 */
@RestController
@RequestMapping("/api/v1/integrations/google-drive")
@RequiredArgsConstructor
public class GoogleDriveIntegrationController {

    private final DriveConnectionService connectionService;
    private final CurrentTenant currentTenant;

    @GetMapping("/status")
    public DriveStatusResponse status() {
        return connectionService.status(currentTenant.organizationId());
    }

    @GetMapping("/authorize-url")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AuthorizeUrlResponse authorizeUrl() {
        String url = connectionService.buildAuthorizeUrl(
                currentTenant.organizationId(), currentTenant.userId());
        return new AuthorizeUrlResponse(url);
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        String redirect = connectionService.handleCallback(code, state, error);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirect)).build();
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    public void disconnect() {
        connectionService.disconnect(currentTenant.organizationId());
    }
}
