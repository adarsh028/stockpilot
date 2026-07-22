package com.stockpilot.sales;

import com.stockpilot.channel.Channel;
import com.stockpilot.channel.ChannelRepository;
import com.stockpilot.product.Product;
import com.stockpilot.product.ProductRepository;
import com.stockpilot.product.Sku;
import com.stockpilot.product.SkuRepository;
import com.stockpilot.sales.dto.SaleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SaleMapper {

    private final ChannelRepository channelRepository;
    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;

    public SaleResponse toResponse(Sale sale) {
        return toResponse(sale, List.of());
    }

    public SaleResponse toResponse(Sale sale, List<String> warnings) {
        Channel channel = channelRepository
                .findByIdAndOrganizationId(sale.getChannelId(), sale.getOrganizationId()).orElse(null);
        Sku sku = skuRepository
                .findByIdAndOrganizationId(sale.getSkuId(), sale.getOrganizationId()).orElse(null);
        String productName = "";
        if (sku != null) {
            Product product = productRepository
                    .findByIdAndOrganizationId(sku.getProductId(), sale.getOrganizationId()).orElse(null);
            if (product != null) {
                productName = product.getName();
            }
        }
        return new SaleResponse(
                sale.getId().toString(),
                sale.getChannelId().toString(),
                channel != null ? channel.getName() : "",
                sale.getSkuId().toString(),
                sku != null ? sku.getSku() : "",
                productName,
                sale.getQuantity(),
                sale.getUnitPrice(),
                sale.getTotalAmount(),
                sale.getSaleDate(),
                sale.getMarketplaceOrderId(),
                sale.getSource().name(),
                sale.getStatus().name(),
                warnings
        );
    }
}
