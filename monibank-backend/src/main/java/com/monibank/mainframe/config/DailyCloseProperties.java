package com.monibank.mainframe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Locale;

@ConfigurationProperties(prefix = "monibank.daily-close")
public record DailyCloseProperties(
        boolean enabled,
        boolean catchUpOnStartup,
        String cron,
        String zone,
        String currency,
        Integer interestRateBasisPoints,
        Duration batchTimeout
) {

    public DailyCloseProperties {
        cron = defaultIfBlank(cron, "0 5 0 * * *");
        zone = defaultIfBlank(zone, "Europe/Warsaw");
        currency = defaultIfBlank(currency, "EUR")
                .toUpperCase(Locale.ROOT);
        interestRateBasisPoints = interestRateBasisPoints == null
                ? 500
                : interestRateBasisPoints;
        batchTimeout = defaultDuration(batchTimeout, Duration.ofMinutes(2));

        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Daily close currency must contain three letters."
            );
        }

        if (interestRateBasisPoints < 1
                || interestRateBasisPoints > 99999) {
            throw new IllegalArgumentException(
                    "Interest rate must be between 1 and 99999 basis points."
            );
        }

        ZoneId.of(zone);
    }

    public ZoneId zoneId() {
        return ZoneId.of(zone);
    }

    private static String defaultIfBlank(
            String value,
            String defaultValue
    ) {
        return value == null || value.isBlank()
                ? defaultValue
                : value;
    }

    private static Duration defaultDuration(
            Duration value,
            Duration defaultValue
    ) {
        return value == null ? defaultValue : value;
    }
}
