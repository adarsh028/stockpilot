package com.stockpilot.integration.drive.dto;

import java.time.Instant;

/**
 * Connection status for the current tenant's Google Drive.
 *
 * @param configured whether the server has OAuth credentials at all (if false, the
 *                   Connect button should be disabled with an explanation)
 */
public record DriveStatusResponse(
        boolean connected,
        boolean configured,
        String email,
        Instant connectedAt
) {
}
