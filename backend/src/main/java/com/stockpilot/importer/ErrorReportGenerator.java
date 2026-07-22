package com.stockpilot.importer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Writes a CSV of failed import rows to the configured storage dir and returns a
 * reference the download endpoint can resolve. Storage is local filesystem for now,
 * swappable for object storage behind this class later.
 */
@Slf4j
@Component
public class ErrorReportGenerator {

    private final Path baseDir;

    public ErrorReportGenerator(@Value("${app.storage.import-reports-dir}") String dir) {
        this.baseDir = Paths.get(dir).toAbsolutePath();
    }

    public String generate(UUID batchId, List<ImportRowError> errors) {
        try {
            Files.createDirectories(baseDir);
            String fileName = "import-errors-" + batchId + ".csv";
            Path file = baseDir.resolve(fileName);

            Set<String> dataColumns = new LinkedHashSet<>();
            for (ImportRowError e : errors) {
                dataColumns.addAll(e.rawValues().keySet());
            }

            List<String> header = new ArrayList<>();
            header.add("rowNumber");
            header.add("error");
            header.addAll(dataColumns);

            try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(file, StandardCharsets.UTF_8),
                    CSVFormat.DEFAULT.builder().setHeader(header.toArray(new String[0])).build())) {
                for (ImportRowError e : errors) {
                    List<Object> record = new ArrayList<>();
                    record.add(e.rowNumber());
                    record.add(e.message());
                    for (String col : dataColumns) {
                        record.add(e.rawValues().getOrDefault(col, ""));
                    }
                    printer.printRecord(record);
                }
            }
            return fileName;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to write import error report", ex);
        }
    }

    public Path resolve(String reportFileName) {
        return baseDir.resolve(reportFileName);
    }
}
