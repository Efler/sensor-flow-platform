package org.eflerrr.sfp.app.alerter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record Alert(
        @JsonProperty("device_id")
        String deviceId,
        @JsonProperty("metric_name")
        String metricName,
        @JsonProperty("metric_value")
        Double metricValue,
        @JsonProperty("src_timestamp")
        Instant srcTimestamp,
        @JsonProperty("thresholds")
        Thresholds thresholds,
        @JsonProperty("alert_type")
        AlertType alertType
) {

    public record Thresholds(
            @JsonProperty("lower")
            Double lower,
            @JsonProperty("upper")
            Double upper
    ) {
    }

    public enum AlertType {
        BROKEN_LOWER_THRESHOLD,
        BROKEN_UPPER_THRESHOLD
    }
}
