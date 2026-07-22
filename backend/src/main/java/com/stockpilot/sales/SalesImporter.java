package com.stockpilot.sales;

import com.stockpilot.channel.Channel;
import com.stockpilot.channel.ChannelRepository;
import com.stockpilot.importer.ImportRow;
import com.stockpilot.importer.RowValidationException;
import com.stockpilot.product.Sku;
import com.stockpilot.product.SkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Persists a single sales-import row in its own transaction, delegating the inventory
 * side effects to {@link SaleService#record} so import and manual entry share one path.
 */
@Service
@RequiredArgsConstructor
public class SalesImporter {

    private final SkuRepository skuRepository;
    private final ChannelRepository channelRepository;
    private final SaleService saleService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importRow(UUID orgId, UUID channelIdOverride, ImportRow row) {
        String skuCode = row.get("sku");
        String channelValue = row.get("channel");
        if (isBlank(skuCode)) {
            throw new RowValidationException("Missing required column: sku");
        }

        Sku sku = skuRepository.findByOrganizationIdAndSkuIgnoreCase(orgId, skuCode)
                .orElseThrow(() -> new RowValidationException("Unknown SKU: " + skuCode));

        UUID channelId = channelIdOverride;
        if (channelId == null) {
            if (isBlank(channelValue)) {
                throw new RowValidationException("Missing required column: channel");
            }
            Channel channel = channelRepository.findByOrganizationIdAndCodeIgnoreCase(orgId, channelValue)
                    .or(() -> channelRepository.findByOrganizationIdAndNameIgnoreCase(orgId, channelValue))
                    .orElseThrow(() -> new RowValidationException("Unknown channel: " + channelValue));
            channelId = channel.getId();
        }

        int quantity = parsePositiveInt(row.get("quantity"));
        BigDecimal unitPrice = parseMoney(row.get("unitprice"));
        Instant saleDate = parseDate(row.get("saledate"));
        String orderId = row.get("marketplaceorderid");

        saleService.record(orgId, channelId, sku.getId(), quantity, unitPrice, saleDate, orderId, SaleSource.IMPORT);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private int parsePositiveInt(String value) {
        if (isBlank(value)) {
            throw new RowValidationException("Missing required column: quantity");
        }
        try {
            int v = Integer.parseInt(value.trim());
            if (v <= 0) {
                throw new RowValidationException("quantity must be greater than zero");
            }
            return v;
        } catch (NumberFormatException e) {
            throw new RowValidationException("Invalid quantity: " + value);
        }
    }

    private BigDecimal parseMoney(String value) {
        if (isBlank(value)) {
            throw new RowValidationException("Missing required column: unitPrice");
        }
        try {
            BigDecimal bd = new BigDecimal(value.replace(",", "").trim());
            if (bd.signum() < 0) {
                throw new RowValidationException("unitPrice cannot be negative");
            }
            return bd;
        } catch (NumberFormatException e) {
            throw new RowValidationException("Invalid unitPrice: " + value);
        }
    }

    private Instant parseDate(String value) {
        if (isBlank(value)) {
            throw new RowValidationException("Missing required column: saleDate");
        }
        String v = value.trim();
        try {
            return Instant.parse(v);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDateTime.parse(v).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDate.parse(v).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException e) {
            throw new RowValidationException("Invalid saleDate (use yyyy-MM-dd): " + value);
        }
    }
}
