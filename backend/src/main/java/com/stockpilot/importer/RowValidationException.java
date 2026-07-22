package com.stockpilot.importer;

/** Thrown per-row during import when a data row fails validation. */
public class RowValidationException extends RuntimeException {

    public RowValidationException(String message) {
        super(message);
    }
}
