package com.stockpilot.importer;

import com.stockpilot.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "import_batches")
public class ImportBatch extends TenantEntity {

    @Column(name = "channel_id")
    private UUID channelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportKind kind;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status = ImportStatus.PROCESSING;

    @Column(name = "rows_total", nullable = false)
    private int rowsTotal = 0;

    @Column(name = "rows_success", nullable = false)
    private int rowsSuccess = 0;

    @Column(name = "rows_failed", nullable = false)
    private int rowsFailed = 0;

    @Column(name = "error_report_url")
    private String errorReportUrl;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;
}
