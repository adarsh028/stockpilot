package com.stockpilot.inventory;

import com.stockpilot.common.dto.PageResponse;
import com.stockpilot.inventory.dto.AdjustStockRequest;
import com.stockpilot.inventory.dto.InventoryItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public PageResponse<InventoryItemResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return inventoryService.list(pageable);
    }

    @GetMapping("/low-stock")
    public List<InventoryItemResponse> lowStock() {
        return inventoryService.lowStock();
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public InventoryItemResponse adjust(@Valid @RequestBody AdjustStockRequest req) {
        return inventoryService.adjust(req);
    }
}
