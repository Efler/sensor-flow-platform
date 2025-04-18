package org.eflerrr.sfp.sparkjobs.alerts.thresholder.model.impl;

import org.eflerrr.sfp.sparkjobs.alerts.thresholder.model.Rule;

import java.sql.Timestamp;
import java.time.LocalDate;

public class LowerRule extends Rule {
    public LowerRule(
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
        super(deviceId, modelId, installDate, envTemp, usageHours, a0, a1, a2, a3);
    }

    @Override
    public double threshold(Timestamp ts) {
        double age = ageDays(ts);
        return a0 + a1 * envTemp + a2 * Math.log(usageHours + 1) + a3 * age;
    }
}
