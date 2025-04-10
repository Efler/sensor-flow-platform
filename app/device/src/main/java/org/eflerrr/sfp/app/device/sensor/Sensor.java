package org.eflerrr.sfp.app.device.sensor;

import org.eflerrr.sfp.app.device.model.Metric;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Random;

public class Sensor {
    private final String deviceId;
    private final String metricName;
    private final double rangeMinValue;
    private final double rangeMaxValue;
    private final Random random = new Random();

    public Sensor(String deviceId, String metricName, double rangeMinValue, double rangeMaxValue) {
        this.deviceId = deviceId;
        this.metricName = metricName;
        this.rangeMinValue = rangeMinValue;
        this.rangeMaxValue = rangeMaxValue;
    }

    public Metric generateMetric() {
        double value = rangeMinValue + (rangeMaxValue - rangeMinValue) * random.nextDouble();
        return new Metric(
                deviceId, metricName, value, OffsetDateTime.now(ZoneOffset.UTC).toInstant()
        );
    }
}
