package org.eflerrr.sfp.sparkjobs.alerts.thresholder.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public record Rule(
        String deviceId,
        String modelId,
        LocalDate installDate,
        double envTemp,
        double usageHours,
        double a0,
        double a1,
        double a2,
        double a3
) implements Serializable {
    private double ageDays(Timestamp ts) {
        LocalDate now = ts.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        return ChronoUnit.DAYS.between(installDate, now);
    }
    public double upper(Timestamp ts) {
        double age = ageDays(ts);
        return a0 + a1 * envTemp + a2 * Math.sqrt(usageHours) + a3 * age;
    }
    public double lower(Timestamp ts) {
        double age = ageDays(ts);
        return a0 - a1 * envTemp - a2 * Math.log(usageHours + 1) - a3 * age;
    }
}
