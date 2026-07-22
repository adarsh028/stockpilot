package com.stockpilot.importer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvRowParser implements RowParser {

    @Override
    public List<ImportRow> parse(InputStream in) throws IOException {
        List<ImportRow> rows = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
             CSVParser parser = format.parse(reader)) {
            List<String> headers = parser.getHeaderNames();
            for (CSVRecord record : parser) {
                Map<String, String> values = new LinkedHashMap<>();
                for (String header : headers) {
                    values.put(normalize(header), record.isSet(header) ? record.get(header) : null);
                }
                // rowNumber: header is line 1, first data record is line 2
                rows.add(new ImportRow((int) record.getRecordNumber() + 1, values));
            }
        }
        return rows;
    }

    private String normalize(String header) {
        return header == null ? "" : header.trim().toLowerCase();
    }
}
