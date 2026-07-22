package com.stockpilot.importer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XlsxRowParser implements RowParser {

    private final DataFormatter formatter = new DataFormatter();

    @Override
    public List<ImportRow> parse(InputStream in) throws IOException {
        List<ImportRow> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return rows;
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return rows;
            }
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                headers.add(cell == null ? "" : formatter.formatCellValue(cell).trim().toLowerCase());
            }

            for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isEmpty(row)) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c);
                    values.put(headers.get(c), cell == null ? null : formatter.formatCellValue(cell).trim());
                }
                rows.add(new ImportRow(r + 1, values));
            }
        }
        return rows;
    }

    private boolean isEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
