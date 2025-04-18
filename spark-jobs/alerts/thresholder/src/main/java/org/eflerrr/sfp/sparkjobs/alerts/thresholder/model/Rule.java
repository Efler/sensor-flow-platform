package org.eflerrr.sfp.sparkjobs.alerts.thresholder.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public abstract class Rule implements Serializable {
    protected final String deviceId;
    protected final String modelId;
    protected final LocalDate installDate;
    protected final double envTemp;
    protected final double usageHours;
    protected final double a0;
    protected final double a1;
    protected final double a2;
    protected final double a3;

    public Rule(
            String deviceId,
            String modelId,
            LocalDate installDate,
            double envTemp,
            double usageHours,
            double a0,
            double a1,
            double a2,
            double a3
    ) {
        this.deviceId = deviceId;
        this.modelId = modelId;
        this.installDate = installDate;
        this.envTemp = envTemp;
        this.usageHours = usageHours;
        this.a0 = a0;
        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;
    }

    protected double ageDays(Timestamp ts) {
        LocalDate now = ts.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        return ChronoUnit.DAYS.between(installDate, now);
    }

    public abstract double threshold(Timestamp ts);

    @Override
    public String toString() {
        return "Rule{" +
               "deviceId='" + deviceId + '\'' +
               ", modelId='" + modelId + '\'' +
               ", installDate=" + installDate +
               ", envTemp=" + envTemp +
               ", usageHours=" + usageHours +
               ", a0=" + a0 +
               ", a1=" + a1 +
               ", a2=" + a2 +
               ", a3=" + a3 +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rule rule)) return false;
        return Double.compare(rule.envTemp, envTemp) == 0 &&
               Double.compare(rule.usageHours, usageHours) == 0 &&
               Double.compare(rule.a0, a0) == 0 &&
               Double.compare(rule.a1, a1) == 0 &&
               Double.compare(rule.a2, a2) == 0 &&
               Double.compare(rule.a3, a3) == 0 &&
               Objects.equals(deviceId, rule.deviceId) &&
               Objects.equals(modelId, rule.modelId) &&
               Objects.equals(installDate, rule.installDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, modelId, installDate, envTemp, usageHours, a0, a1, a2, a3);
    }
}
