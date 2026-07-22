package com.stockpilot.importer;

import com.stockpilot.common.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class RowParserFactory {

    public RowParser forFile(String fileName) {
        if (fileName == null) {
            throw new ValidationException("Missing file name");
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".csv")) {
            return new CsvRowParser();
        }
        if (lower.endsWith(".xlsx")) {
            return new XlsxRowParser();
        }
        throw new ValidationException("Unsupported file type. Please upload a .csv or .xlsx file");
    }
}
