package com.stockpilot.importer;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    @GetMapping("/products-import-sample")
    public ResponseEntity<Resource> productsTemplate() {
        return serve("templates/products_import_sample.csv", "products_import_sample.csv");
    }

    @GetMapping("/sales-import-sample")
    public ResponseEntity<Resource> salesTemplate() {
        return serve("templates/sales_import_sample.csv", "sales_import_sample.csv");
    }

    private ResponseEntity<Resource> serve(String classpath, String downloadName) {
        Resource resource = new ClassPathResource(classpath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}
