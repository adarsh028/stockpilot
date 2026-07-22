package com.stockpilot.importer;

import com.stockpilot.common.dto.PageResponse;
import com.stockpilot.importer.dto.ImportBatchResponse;
import com.stockpilot.sales.SalesImporter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final ProductImporter productImporter;
    private final SalesImporter salesImporter;
    private final ErrorReportGenerator errorReportGenerator;

    @PostMapping(value = "/products/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ImportBatchResponse importProducts(@RequestParam("file") MultipartFile file) {
        return importService.run(ImportKind.PRODUCTS, null, file, productImporter::importRow);
    }

    @PostMapping(value = "/sales/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','STAFF')")
    public ImportBatchResponse importSales(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "channelId", required = false) UUID channelId) {
        return importService.run(ImportKind.SALES, channelId, file,
                (orgId, row) -> salesImporter.importRow(orgId, channelId, row));
    }

    @GetMapping("/import-batches")
    public PageResponse<ImportBatchResponse> listBatches(
            @RequestParam(required = false) ImportKind kind,
            @PageableDefault(size = 20) Pageable pageable) {
        return importService.list(kind, pageable);
    }

    @GetMapping("/import-batches/{id}")
    public ImportBatchResponse getBatch(@PathVariable UUID id) {
        return importService.toResponse(importService.getOwned(id));
    }

    @GetMapping("/import-batches/{id}/error-report")
    public ResponseEntity<Resource> downloadErrorReport(@PathVariable UUID id) {
        ImportBatch batch = importService.getOwned(id);
        if (batch.getErrorReportUrl() == null) {
            return ResponseEntity.notFound().build();
        }
        Path file = errorReportGenerator.resolve(batch.getErrorReportUrl());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + batch.getErrorReportUrl() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new FileSystemResource(file));
    }
}
