package com.stockpilot.inventory;

import com.stockpilot.email.BrevoEmailService;
import com.stockpilot.organization.Organization;
import com.stockpilot.organization.OrganizationRepository;
import com.stockpilot.product.Sku;
import com.stockpilot.product.SkuRepository;
import com.stockpilot.user.User;
import com.stockpilot.user.UserRepository;
import com.stockpilot.user.UserRole;
import com.stockpilot.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Best-effort low-stock alert. Runs asynchronously so it never blocks the sale that
 * triggered it; email delivery itself falls back to logging when Brevo is unconfigured.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LowStockNotifier {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final SkuRepository skuRepository;
    private final BrevoEmailService emailService;

    @Async
    public void notifyLowStock(UUID orgId, UUID skuId, int quantityOnHand, int reorderLevel) {
        Organization org = organizationRepository.findById(orgId).orElse(null);
        if (org == null) {
            return;
        }
        Sku sku = skuRepository.findByIdAndOrganizationId(skuId, orgId).orElse(null);
        String skuLabel = sku != null ? sku.getSku() : skuId.toString();

        String body = "<ul><li><b>" + skuLabel + "</b> — on hand: " + quantityOnHand
                + ", reorder level: " + reorderLevel + "</li></ul>";

        userRepository.findByOrganizationId(orgId, Pageable.ofSize(50)).stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .filter(u -> u.getRole() == UserRole.OWNER || u.getRole() == UserRole.ADMIN)
                .map(User::getEmail)
                .distinct()
                .forEach(email -> emailService.sendLowStockAlert(email, null, org.getName(), body));

        log.info("Low-stock alert dispatched for org={} sku={} qty={}", orgId, skuLabel, quantityOnHand);
    }
}
