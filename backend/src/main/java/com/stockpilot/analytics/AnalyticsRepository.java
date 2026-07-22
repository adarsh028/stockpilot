package com.stockpilot.analytics;

import com.stockpilot.analytics.dto.SalesByChannelResponse;
import com.stockpilot.analytics.dto.SalesTrendPoint;
import com.stockpilot.analytics.dto.TopProductResponse;
import com.stockpilot.common.util.DateRange;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Read-only aggregate queries. All are org-scoped and exclude RETURNED sales from
 * revenue/units. Native SQL is used because these are set-based aggregates that would
 * be wasteful to compute in the JVM.
 */
@Repository
@RequiredArgsConstructor
public class AnalyticsRepository {

    private final JdbcTemplate jdbc;

    public Totals totals(UUID orgId, DateRange range) {
        String sql = """
                SELECT COUNT(*) AS order_count,
                       COALESCE(SUM(quantity), 0) AS units,
                       COALESCE(SUM(total_amount), 0) AS revenue
                FROM sales
                WHERE organization_id = ? AND status = 'COMPLETED'
                  AND sale_date >= ? AND sale_date < ?
                """;
        return jdbc.queryForObject(sql, (rs, n) -> new Totals(
                rs.getLong("order_count"),
                rs.getLong("units"),
                rs.getBigDecimal("revenue")
        ), orgId, Timestamp.from(range.from()), Timestamp.from(range.to()));
    }

    public List<SalesByChannelResponse> salesByChannel(UUID orgId, DateRange range) {
        String sql = """
                SELECT c.id AS channel_id, c.name AS channel_name,
                       COALESCE(SUM(s.quantity), 0) AS units,
                       COALESCE(SUM(s.total_amount), 0) AS revenue
                FROM channels c
                LEFT JOIN sales s ON s.channel_id = c.id
                     AND s.status = 'COMPLETED'
                     AND s.organization_id = c.organization_id
                     AND s.sale_date >= ? AND s.sale_date < ?
                WHERE c.organization_id = ?
                GROUP BY c.id, c.name
                ORDER BY revenue DESC, c.name
                """;
        return jdbc.query(sql, (rs, n) -> new SalesByChannelResponse(
                rs.getString("channel_id"),
                rs.getString("channel_name"),
                rs.getLong("units"),
                rs.getBigDecimal("revenue")
        ), Timestamp.from(range.from()), Timestamp.from(range.to()), orgId);
    }

    public List<SalesTrendPoint> salesTrend(UUID orgId, DateRange range, String granularityLiteral) {
        String sql = """
                SELECT date_trunc(?, sale_date) AS bucket,
                       COALESCE(SUM(quantity), 0) AS units,
                       COALESCE(SUM(total_amount), 0) AS revenue
                FROM sales
                WHERE organization_id = ? AND status = 'COMPLETED'
                  AND sale_date >= ? AND sale_date < ?
                GROUP BY bucket
                ORDER BY bucket
                """;
        return jdbc.query(sql, (rs, n) -> new SalesTrendPoint(
                rs.getTimestamp("bucket").toInstant().toString(),
                rs.getLong("units"),
                rs.getBigDecimal("revenue")
        ), granularityLiteral, orgId, Timestamp.from(range.from()), Timestamp.from(range.to()));
    }

    public List<TopProductResponse> topProducts(UUID orgId, DateRange range, int limit) {
        String sql = """
                SELECT p.id AS product_id, p.name AS product_name, sk.sku AS sku,
                       COALESCE(SUM(s.quantity), 0) AS units,
                       COALESCE(SUM(s.total_amount), 0) AS revenue
                FROM sales s
                JOIN skus sk ON sk.id = s.sku_id
                JOIN products p ON p.id = sk.product_id
                WHERE s.organization_id = ? AND s.status = 'COMPLETED'
                  AND s.sale_date >= ? AND s.sale_date < ?
                GROUP BY p.id, p.name, sk.sku
                ORDER BY revenue DESC
                LIMIT ?
                """;
        return jdbc.query(sql, (rs, n) -> new TopProductResponse(
                rs.getString("product_id"),
                rs.getString("product_name"),
                rs.getString("sku"),
                rs.getLong("units"),
                rs.getBigDecimal("revenue")
        ), orgId, Timestamp.from(range.from()), Timestamp.from(range.to()), limit);
    }

    public record Totals(long orderCount, long units, BigDecimal revenue) {
    }
}
