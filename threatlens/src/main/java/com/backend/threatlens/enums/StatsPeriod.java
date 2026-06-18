package com.backend.threatlens.enums;

import java.time.LocalDateTime;
import java.util.Optional;

public enum StatsPeriod {
    DAY(1),
    WEEK(7),
    MONTH(30),
    YEAR(365),
    ALL(null);

    private final Integer days;

    StatsPeriod(Integer days) {
        this.days = days;
    }

    public Optional<LocalDateTime> since() {
        return days == null ? Optional.empty()
                            : Optional.of(LocalDateTime.now().minusDays(days));
    }
}
