package com.stockpilot.importer;

import java.util.Map;

public record ImportRowError(int rowNumber, String message, Map<String, String> rawValues) {
}
