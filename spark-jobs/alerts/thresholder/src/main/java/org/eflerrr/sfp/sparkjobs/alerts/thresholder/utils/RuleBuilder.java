package org.eflerrr.sfp.sparkjobs.alerts.thresholder.utils;

import org.eflerrr.sfp.sparkjobs.alerts.thresholder.model.Rule;
import org.eflerrr.sfp.sparkjobs.alerts.thresholder.model.ThresholdType;
import org.eflerrr.sfp.sparkjobs.alerts.thresholder.model.impl.LowerRule;
import org.eflerrr.sfp.sparkjobs.alerts.thresholder.model.impl.UpperRule;

import java.time.LocalDate;

public class RuleBuilder {
    private String deviceId = "stub";
    private String modelId = "stub";
    private LocalDate installDate = LocalDate.of(2000, 1, 1);
    private double envTemp = 0.0;
    private double usageHours = 0.0;
    private double a0 = 0.0;
    private double a1 = 0.0;
    private double a2 = 0.0;
    private double a3 = 0.0;

    private ThresholdType type = null;

    public static RuleBuilder getInstance() {
        return new RuleBuilder();
    }

    public Rule build() {
        return switch (type) {
            case LOWER_THRESHOLD_TYPE -> new LowerRule(
                    deviceId, modelId, installDate, envTemp, usageHours, a0, a1, a2, a3
            );
            case UPPER_THRESHOLD_TYPE -> new UpperRule(
                    deviceId, modelId, installDate, envTemp, usageHours, a0, a1, a2, a3
            );
        };
    }

    public RuleBuilder withDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public RuleBuilder withModelId(String modelId) {
        this.modelId = modelId;
        return this;
    }

    public RuleBuilder withInstallDate(LocalDate installDate) {
        this.installDate = installDate;
        return this;
    }

    public RuleBuilder withEnvTemp(double envTemp) {
        this.envTemp = envTemp;
        return this;
    }

    public RuleBuilder withUsageHours(double usageHours) {
        this.usageHours = usageHours;
        return this;
    }

    public RuleBuilder withA0(double a0) {
        this.a0 = a0;
        return this;
    }

    public RuleBuilder withA1(double a1) {
        this.a1 = a1;
        return this;
    }

    public RuleBuilder withA2(double a2) {
        this.a2 = a2;
        return this;
    }

    public RuleBuilder withA3(double a3) {
        this.a3 = a3;
        return this;
    }

    public RuleBuilder withType(ThresholdType type) {
        this.type = type;
        return this;
    }

}
