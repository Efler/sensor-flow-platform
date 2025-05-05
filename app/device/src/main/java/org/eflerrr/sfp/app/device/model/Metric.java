package org.eflerrr.sfp.app.device.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record Metric(
        @JsonProperty("device_id")
        String deviceId,
        @JsonProperty("metric_name")
        String metricName,
        @JsonProperty("metric_value")
        Double metricValue,
        @JsonProperty("src_timestamp")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime srcTimestamp
) {
}
