package com.stockpilot.seed;

import java.util.List;
import java.util.Map;

public final class DemoDataConstants {

    private DemoDataConstants() {
    }

    public static final String DEMO_ORG_NAME = "StockPilot Demo";
    public static final String DEMO_SLUG = "demo";
    public static final String OWNER_EMAIL = "owner@demo.stockpilot.io";
    public static final String ADMIN_EMAIL = "admin@demo.stockpilot.io";
    public static final String STAFF_EMAIL = "staff@demo.stockpilot.io";
    public static final String DEMO_PASSWORD = "Demo@12345";

    public record SkuSeed(String code, Map<String, String> attributes, double cost, double price,
                          int quantity, int reorder) {
    }

    public record ProductSeed(String name, String category, String brand, List<SkuSeed> skus) {
    }

    private static SkuSeed sku(String code, double cost, double price, int qty, int reorder, String... kv) {
        Map<String, String> attrs = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            attrs.put(kv[i], kv[i + 1]);
        }
        return new SkuSeed(code, attrs, cost, price, qty, reorder);
    }

    public static final ProductSeed[] PRODUCTS = new ProductSeed[]{
            new ProductSeed("Classic Cotton T-Shirt", "Apparel", "Acme", List.of(
                    sku("TSHIRT-BLK-S", 180, 499, 120, 20, "size", "S", "color", "Black"),
                    sku("TSHIRT-BLK-M", 180, 499, 15, 20, "size", "M", "color", "Black"),
                    sku("TSHIRT-BLK-L", 180, 499, 95, 20, "size", "L", "color", "Black"))),
            new ProductSeed("Slim Fit Jeans", "Apparel", "Denimery", List.of(
                    sku("JEANS-32", 650, 1799, 60, 15, "waist", "32"),
                    sku("JEANS-34", 650, 1799, 8, 15, "waist", "34"))),
            new ProductSeed("Running Shoes", "Footwear", "Strider", List.of(
                    sku("SHOE-8", 1200, 2999, 40, 10, "size", "8"),
                    sku("SHOE-9", 1200, 2999, 35, 10, "size", "9"),
                    sku("SHOE-10", 1200, 2999, 5, 10, "size", "10"))),
            new ProductSeed("Wireless Earbuds", "Electronics", "SoundPods", List.of(
                    sku("EARBUDS-WHT", 650, 1499, 200, 30, "color", "White"),
                    sku("EARBUDS-BLK", 650, 1499, 180, 30, "color", "Black"))),
            new ProductSeed("Smart Watch", "Electronics", "PulseTech", List.of(
                    sku("WATCH-BLK", 2200, 4999, 50, 10, "color", "Black"),
                    sku("WATCH-SLV", 2200, 4999, 25, 10, "color", "Silver"))),
            new ProductSeed("Stainless Water Bottle", "Home", "HydroLife", List.of(
                    sku("BOTTLE-750", 220, 599, 300, 40))),
            new ProductSeed("Yoga Mat", "Fitness", "ZenFlex", List.of(
                    sku("YOGAMAT-PUR", 400, 999, 70, 15, "color", "Purple"),
                    sku("YOGAMAT-BLU", 400, 999, 12, 15, "color", "Blue"))),
            new ProductSeed("Ceramic Coffee Mug", "Home", "BrewHaus", List.of(
                    sku("MUG-350", 90, 299, 500, 50))),
            new ProductSeed("Bluetooth Speaker", "Electronics", "SoundPods", List.of(
                    sku("SPEAKER-MINI", 900, 1999, 90, 20))),
            new ProductSeed("Leather Wallet", "Accessories", "Craftsman", List.of(
                    sku("WALLET-BRN", 350, 899, 110, 20, "color", "Brown"),
                    sku("WALLET-BLK", 350, 899, 130, 20, "color", "Black"))),
            new ProductSeed("Backpack 25L", "Accessories", "TrailMate", List.of(
                    sku("BACKPACK-GRY", 800, 1999, 45, 10, "color", "Grey"))),
            new ProductSeed("Sunglasses", "Accessories", "SunShade", List.of(
                    sku("SUNGLASS-AVI", 500, 1299, 60, 15, "style", "Aviator"))),
            new ProductSeed("Cotton Hoodie", "Apparel", "Acme", List.of(
                    sku("HOODIE-M", 550, 1499, 40, 10, "size", "M"),
                    sku("HOODIE-L", 550, 1499, 6, 10, "size", "L"))),
            new ProductSeed("Desk Lamp LED", "Home", "BrightSpace", List.of(
                    sku("LAMP-WHT", 450, 1199, 75, 15, "color", "White"))),
            new ProductSeed("Notebook A5", "Stationery", "PaperCo", List.of(
                    sku("NOTEBOOK-A5", 60, 199, 800, 100)))
    };
}
