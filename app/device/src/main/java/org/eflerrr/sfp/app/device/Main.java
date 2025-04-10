package org.eflerrr.sfp.app.device;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.eflerrr.sfp.app.device.sensor.SensorsFactory;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Config config = ConfigFactory.load();
        Config kafkaConfig = config.getConfig("kafka");

        Device device = new Device(
                SensorsFactory.createSensors(config),
                config.getLong("generation-interval"),
                config.getInt("threads-count"),
                kafkaConfig.getString("topic"),
                Map.of(
                        "bootstrap.servers", kafkaConfig.getString("bootstrap-servers"),
                        "client.id", kafkaConfig.getString("client-id"),
                        "acks", kafkaConfig.getString("acks")
                )
        );

        device.start();
        Runtime.getRuntime().addShutdownHook(new Thread(device::stop));

        device.awaitTermination();
    }
}
