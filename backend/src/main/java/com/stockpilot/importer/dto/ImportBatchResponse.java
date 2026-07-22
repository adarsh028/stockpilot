package com.stockpilot.importer.dto;

import java.time.Instant;

public record ImportBatchResponse(
        String id,
        String kind,
        String channelId,
        String fileName,
        String status,
        int rowsTotal,
        int rowsSuccess,
        int rowsFailed,
        boolean hasErrorReport,
        Instant createdAt
) {
}
