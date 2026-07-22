package com.stockpilot.importer;

import java.util.Map;

/**
 * A single parsed data row: the 1-based row number in the source file (header is row 1)
 * and the header→value cell map.
 */
public record ImportRow(int rowNumber, Map<String, String> values) {

    public String get(String column) {
        String v = values.get(column);
        return v == null ? null : v.trim();
    }
}
