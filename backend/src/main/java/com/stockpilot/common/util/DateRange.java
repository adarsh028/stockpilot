package com.stockpilot.common.util;

import java.time.Instant;

public record DateRange(Instant from, Instant to) {

    public long durationSeconds() {
        return to.getEpochSecond() - from.getEpochSecond();
    }

    /** The equal-length window immediately preceding this one (for period-over-period comparison). */
    public DateRange previousPeriod() {
        long duration = durationSeconds();
        return new DateRange(from.minusSeconds(duration), from);
    }
}
