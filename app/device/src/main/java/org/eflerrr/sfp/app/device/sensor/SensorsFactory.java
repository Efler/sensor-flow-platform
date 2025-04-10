package org.eflerrr.sfp.app.device.sensor;

import com.typesafe.config.Config;

import java.util.HashSet;
import java.util.Set;

public class SensorsFactory {
    public static Set<org.eflerrr.sfp.app.device.sensor.Sensor> createSensors(Config config) {
        String deviceId = config.getString("device-id");

        Set<org.eflerrr.sfp.app.device.sensor.Sensor> sensors = new HashSet<>();
        Config sensorsConfig = config.getConfig("sensors");

        for (String metricName : sensorsConfig.root().keySet()) {
            var sensorConfig = sensorsConfig.getConfig(metricName);
            double rangeMinValue = sensorConfig.getDouble("range-min-value");
            double rangeMaxValue = sensorConfig.getDouble("range-max-value");

            sensors.add(new org.eflerrr.sfp.app.device.sensor.Sensor(deviceId, metricName, rangeMinValue, rangeMaxValue));
        }

        return sensors;
    }
}
