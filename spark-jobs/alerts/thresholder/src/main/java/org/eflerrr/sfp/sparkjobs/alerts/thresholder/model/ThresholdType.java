package org.eflerrr.sfp.sparkjobs.alerts.thresholder.model;

public enum ThresholdType {

    LOWER_THRESHOLD_TYPE("lower"),
    UPPER_THRESHOLD_TYPE("upper");

    private final String stringValue;

    ThresholdType(String stringValue) {
        this.stringValue = stringValue;
    }

    public static ThresholdType fromString(String stringValue) {
        for (ThresholdType type : ThresholdType.values()) {
            if (type.stringValue.equals(stringValue)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return stringValue;
    }

}
