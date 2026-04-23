package com.yourcompany.credittracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Component("appDateTime")
public class AppDateTimeService {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

    private final ZoneId zoneId;

    public AppDateTimeService(@Value("${app.time-zone:America/Los_Angeles}") String timeZone) {
        this.zoneId = ZoneId.of(timeZone);
    }

    public String format(LocalDateTime value) {
        if (value == null) {
            return "None";
        }
        String abbreviation = value.atZone(zoneId).format(DateTimeFormatter.ofPattern("z", Locale.US));
        return value.format(DATE_TIME_FORMAT) + " " + abbreviation;
    }

    public String formatDate(LocalDate value) {
        return value == null ? "None" : value.format(DATE_FORMAT);
    }

    public String formatPrice(BigDecimal value) {
        return value == null ? "-" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public String formatUnits(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
