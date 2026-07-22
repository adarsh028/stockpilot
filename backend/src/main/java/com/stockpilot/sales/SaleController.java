package com.stockpilot.sales;

import com.stockpilot.common.dto.PageResponse;
import com.stockpilot.sales.dto.SaleRequest;
import com.stockpilot.sales.dto.SaleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public PageResponse<SaleResponse> list(
            @RequestParam(required = false) UUID channelId,
            @RequestParam(required = false) UUID skuId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "saleDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return saleService.list(channelId, skuId, from, to, status, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','STAFF')")
    public SaleResponse record(@Valid @RequestBody SaleRequest req) {
        return saleService.recordSale(req);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public SaleResponse returnSale(@PathVariable UUID id) {
        return saleService.returnSale(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void delete(@PathVariable UUID id) {
        saleService.delete(id);
    }
}
