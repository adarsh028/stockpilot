package com.stockpilot.seed;

import com.stockpilot.category.Category;
import com.stockpilot.category.CategoryRepository;
import com.stockpilot.channel.Channel;
import com.stockpilot.channel.ChannelRepository;
import com.stockpilot.channel.ChannelSeeder;
import com.stockpilot.inventory.InventoryItem;
import com.stockpilot.inventory.InventoryItemRepository;
import com.stockpilot.organization.Organization;
import com.stockpilot.organization.OrganizationRepository;
import com.stockpilot.product.Product;
import com.stockpilot.product.ProductRepository;
import com.stockpilot.product.Sku;
import com.stockpilot.product.SkuRepository;
import com.stockpilot.sales.Sale;
import com.stockpilot.sales.SaleRepository;
import com.stockpilot.sales.SaleSource;
import com.stockpilot.sales.SaleStatus;
import com.stockpilot.user.User;
import com.stockpilot.user.UserRepository;
import com.stockpilot.user.UserRole;
import com.stockpilot.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Seeds a demo organization with channels, products, inventory and several months of
 * sales so the dashboard is populated on first run. Idempotent: skips if the demo org
 * already exists. Gated on app.seed.demo-enabled (true only in dev/demo profiles).
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.demo-enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ChannelSeeder channelSeeder;
    private final ChannelRepository channelRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SaleRepository saleRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(42);

    @Override
    @Transactional
    public void run(String... args) {
        if (organizationRepository.existsBySlug(DemoDataConstants.DEMO_SLUG)) {
            log.info("Demo data already present (org slug '{}') — skipping seed.", DemoDataConstants.DEMO_SLUG);
            return;
        }
        log.info("Seeding demo data...");

        Organization org = new Organization();
        org.setName(DemoDataConstants.DEMO_ORG_NAME);
        org.setSlug(DemoDataConstants.DEMO_SLUG);
        org = organizationRepository.save(org);
        UUID orgId = org.getId();

        seedUsers(orgId);
        channelSeeder.seedDefaults(orgId);
        List<Channel> channels = channelRepository.findByOrganizationIdOrderByName(orgId).stream()
                .filter(Channel::isActive)
                .toList();

        List<Sku> skus = seedProducts(orgId);
        seedSales(orgId, channels, skus);

        log.info("Demo data seeded: {} products, {} SKUs, {} channels. Login: {} / {}",
                DemoDataConstants.PRODUCTS.length, skus.size(), channels.size(),
                DemoDataConstants.OWNER_EMAIL, DemoDataConstants.DEMO_PASSWORD);
    }

    private void seedUsers(UUID orgId) {
        createUser(orgId, "Demo Owner", DemoDataConstants.OWNER_EMAIL, "+91 90000 00001", UserRole.OWNER);
        createUser(orgId, "Demo Admin", DemoDataConstants.ADMIN_EMAIL, "+91 90000 00002", UserRole.ADMIN);
        createUser(orgId, "Demo Staff", DemoDataConstants.STAFF_EMAIL, "+91 90000 00003", UserRole.STAFF);
    }

    private void createUser(UUID orgId, String name, String email, String phone, UserRole role) {
        User user = new User();
        user.setOrganizationId(orgId);
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(DemoDataConstants.DEMO_PASSWORD));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    private List<Sku> seedProducts(UUID orgId) {
        Map<String, UUID> categoryIdByName = new HashMap<>();
        List<Sku> allSkus = new ArrayList<>();
        for (DemoDataConstants.ProductSeed ps : DemoDataConstants.PRODUCTS) {
            UUID categoryId = categoryIdByName.computeIfAbsent(ps.category(), name -> {
                Category category = new Category();
                category.setOrganizationId(orgId);
                category.setName(name);
                return categoryRepository.save(category).getId();
            });

            Product product = new Product();
            product.setOrganizationId(orgId);
            product.setName(ps.name());
            product.setCategoryId(categoryId);
            product.setBrandName(ps.brand());
            product.setDescription(ps.name() + " by " + ps.brand());
            productRepository.save(product);

            for (DemoDataConstants.SkuSeed ss : ps.skus()) {
                Sku sku = new Sku();
                sku.setOrganizationId(orgId);
                sku.setProductId(product.getId());
                sku.setSku(ss.code());
                sku.setAttributes(new HashMap<>(ss.attributes()));
                sku.setCostPrice(BigDecimal.valueOf(ss.cost()));
                sku.setSellingPrice(BigDecimal.valueOf(ss.price()));
                sku = skuRepository.save(sku);
                allSkus.add(sku);

                InventoryItem item = new InventoryItem();
                item.setOrganizationId(orgId);
                item.setSkuId(sku.getId());
                item.setQuantityOnHand(ss.quantity());
                item.setReorderLevel(ss.reorder());
                inventoryItemRepository.save(item);
            }
        }
        return allSkus;
    }

    private void seedSales(UUID orgId, List<Channel> channels, List<Sku> skus) {
        if (channels.isEmpty() || skus.isEmpty()) {
            return;
        }
        Map<UUID, BigDecimal> priceBySku = new HashMap<>();
        for (Sku sku : skus) {
            priceBySku.put(sku.getId(), sku.getSellingPrice());
        }

        int daysBack = 150;
        Instant now = Instant.now();
        List<Sale> batch = new ArrayList<>();

        for (int d = daysBack; d >= 0; d--) {
            // 2-8 sales per day; ramp volume slightly toward the present for a trend.
            int salesToday = 2 + random.nextInt(7) + (daysBack - d) / 40;
            for (int i = 0; i < salesToday; i++) {
                Sku sku = skus.get(random.nextInt(skus.size()));
                Channel channel = channels.get(random.nextInt(channels.size()));
                int qty = 1 + random.nextInt(4);
                BigDecimal unitPrice = priceBySku.get(sku.getId());
                Instant saleDate = now.minus(d, ChronoUnit.DAYS)
                        .minus(random.nextInt(24), ChronoUnit.HOURS);

                Sale sale = new Sale();
                sale.setOrganizationId(orgId);
                sale.setChannelId(channel.getId());
                sale.setSkuId(sku.getId());
                sale.setQuantity(qty);
                sale.setUnitPrice(unitPrice);
                sale.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(qty)));
                sale.setSaleDate(saleDate);
                sale.setSource(SaleSource.IMPORT);
                sale.setStatus(SaleStatus.COMPLETED);
                batch.add(sale);
            }
        }
        saleRepository.saveAll(batch);
        log.info("Seeded {} demo sales across {} days.", batch.size(), daysBack);
    }
}
