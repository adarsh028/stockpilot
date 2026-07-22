package com.stockpilot.common.util;

import com.stockpilot.common.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class DateRangeResolver {

    private final ZoneId zoneId;

    public DateRangeResolver(@Value("${app.default-zone:Asia/Kolkata}") String zone) {
        this.zoneId = ZoneId.of(zone);
    }

    /**
     * Resolve an analytics date window from either a named preset or an explicit from/to
     * (yyyy-MM-dd). Presets ignore from/to; CUSTOM requires both.
     */
    public DateRange resolve(String preset, String from, String to) {
        LocalDate today = LocalDate.now(zoneId);
        String p = preset == null ? "LAST_30D" : preset.trim().toUpperCase();

        LocalDate startDate;
        LocalDate endDate = today;

        switch (p) {
            case "LAST_7D" -> startDate = today.minusDays(6);
            case "LAST_30D" -> startDate = today.minusDays(29);
            case "LAST_90D" -> startDate = today.minusDays(89);
            case "THIS_MONTH" -> startDate = today.withDayOfMonth(1);
            case "THIS_YEAR" -> startDate = today.withDayOfYear(1);
            case "CUSTOM" -> {
                if (from == null || to == null || from.isBlank() || to.isBlank()) {
                    throw new ValidationException("CUSTOM range requires both 'from' and 'to' (yyyy-MM-dd)");
                }
                startDate = parse(from);
                endDate = parse(to);
                if (startDate.isAfter(endDate)) {
                    throw new ValidationException("'from' must be on or before 'to'");
                }
                if (startDate.isBefore(endDate.minusYears(2))) {
                    throw new ValidationException("Custom range cannot exceed 2 years");
                }
            }
            default -> throw new ValidationException("Unknown preset: " + preset);
        }

        return new DateRange(
                startDate.atStartOfDay(zoneId).toInstant(),
                endDate.plusDays(1).atStartOfDay(zoneId).toInstant()
        );
    }

    private LocalDate parse(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new ValidationException("Invalid date (expected yyyy-MM-dd): " + value);
        }
    }
}
