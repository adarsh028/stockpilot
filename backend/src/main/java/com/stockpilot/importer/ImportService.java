package com.stockpilot.importer;

import com.stockpilot.common.dto.PageResponse;
import com.stockpilot.common.exception.ResourceNotFoundException;
import com.stockpilot.common.exception.ValidationException;
import com.stockpilot.importer.dto.ImportBatchResponse;
import com.stockpilot.tenant.CurrentTenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportBatchRepository batchRepository;
    private final RowParserFactory rowParserFactory;
    private final ErrorReportGenerator errorReportGenerator;
    private final CurrentTenant currentTenant;

    /**
     * Generic import driver: parse the file, run each row through {@code rowHandler}
     * (which persists in its own transaction and throws {@link RowValidationException}
     * on bad data), accumulate per-row errors, and finalize the batch summary.
     */
    public ImportBatchResponse run(ImportKind kind, UUID channelId, MultipartFile file,
                                   BiConsumer<UUID, ImportRow> rowHandler) {
        UUID orgId = currentTenant.organizationId();
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Uploaded file is empty");
        }

        ImportBatch batch = new ImportBatch();
        batch.setOrganizationId(orgId);
        batch.setKind(kind);
        batch.setChannelId(channelId);
        batch.setFileName(file.getOriginalFilename());
        batch.setStatus(ImportStatus.PROCESSING);
        batch.setUploadedBy(currentTenant.userId());
        batch = batchRepository.save(batch);

        List<ImportRow> rows;
        try {
            RowParser parser = rowParserFactory.forFile(file.getOriginalFilename());
            rows = parser.parse(file.getInputStream());
        } catch (IOException ex) {
            batch.setStatus(ImportStatus.FAILED);
            batchRepository.save(batch);
            throw new UncheckedIOException("Could not read uploaded file", ex);
        } catch (ValidationException ex) {
            batch.setStatus(ImportStatus.FAILED);
            batchRepository.save(batch);
            throw ex;
        }

        List<ImportRowError> errors = new ArrayList<>();
        int success = 0;
        for (ImportRow row : rows) {
            try {
                rowHandler.accept(orgId, row);
                success++;
            } catch (RowValidationException ex) {
                errors.add(new ImportRowError(row.rowNumber(), ex.getMessage(), row.values()));
            } catch (Exception ex) {
                log.warn("Unexpected error importing row {}: {}", row.rowNumber(), ex.getMessage());
                errors.add(new ImportRowError(row.rowNumber(),
                        "Unexpected error: " + ex.getMessage(), row.values()));
            }
        }

        batch.setRowsTotal(rows.size());
        batch.setRowsSuccess(success);
        batch.setRowsFailed(errors.size());
        if (!errors.isEmpty()) {
            batch.setErrorReportUrl(errorReportGenerator.generate(batch.getId(), errors));
        }
        batch.setStatus(rows.isEmpty() ? ImportStatus.FAILED : ImportStatus.COMPLETED);
        batch = batchRepository.save(batch);

        return toResponse(batch);
    }

    public PageResponse<ImportBatchResponse> list(ImportKind kind, Pageable pageable) {
        UUID orgId = currentTenant.organizationId();
        var page = (kind == null)
                ? batchRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId, pageable)
                : batchRepository.findByOrganizationIdAndKindOrderByCreatedAtDesc(orgId, kind, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    public ImportBatch getOwned(UUID batchId) {
        UUID orgId = currentTenant.organizationId();
        return batchRepository.findByIdAndOrganizationId(batchId, orgId)
                .orElseThrow(() -> ResourceNotFoundException.of("Import batch", batchId));
    }

    public ImportBatchResponse toResponse(ImportBatch batch) {
        return new ImportBatchResponse(
                batch.getId().toString(),
                batch.getKind().name(),
                batch.getChannelId() != null ? batch.getChannelId().toString() : null,
                batch.getFileName(),
                batch.getStatus().name(),
                batch.getRowsTotal(),
                batch.getRowsSuccess(),
                batch.getRowsFailed(),
                batch.getErrorReportUrl() != null,
                batch.getCreatedAt()
        );
    }
}
