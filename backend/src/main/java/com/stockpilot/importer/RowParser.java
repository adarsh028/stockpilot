package com.stockpilot.importer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface RowParser {

    /** Parse an uploaded file into data rows keyed by header column name. */
    List<ImportRow> parse(InputStream in) throws IOException;
}
