package org.eflerrr.sfp.app.device;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eflerrr.sfp.app.device.model.Metric;
import org.eflerrr.sfp.app.device.sensor.Sensor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Device {
    private final KafkaProducer<String, String> producer;
    private final Set<Sensor> sensors;
    private final String topic;
    private final ScheduledExecutorService executor;
    private final long intervalMillis;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private volatile boolean running = false;

    public Device(Set<Sensor> sensors, long intervalMillis, int threadsCount, String topic, Map<String, Object> kafkaProps) {
        this.sensors = sensors;
        this.intervalMillis = intervalMillis;
        this.executor = Executors.newScheduledThreadPool(threadsCount);
        this.topic = topic;
        objectMapper.registerModule(new JavaTimeModule());

        this.producer = new KafkaProducer<>(kafkaProps, new StringSerializer(), new StringSerializer());
    }

    public void start() {
        if (running) return;
        running = true;

        for (Sensor sensor : sensors) {
            executor.scheduleAtFixedRate(() -> {
                if (!running) return;

                Metric metric = sensor.generateMetric();
                try {
                    ProducerRecord<String, String> record = new ProducerRecord<>(
                            topic, objectMapper.writeValueAsString(metric));
                    producer.send(record, (meta, ex) -> {
                        if (ex != null) {
                            log.error("Error sending record to kafka!", ex);
                        }
                    });
                } catch (JsonProcessingException ex) {
                    log.error("Error while serializing metric to json!", ex);
                }
            }, 0, intervalMillis, TimeUnit.MILLISECONDS);
        }

        log.info("Device started sending metrics...");
    }

    public void stop() {
        if (!running) return;
        running = false;

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        producer.close();
        shutdownLatch.countDown();

        log.info("Device stopped sending metrics...");
    }

    public void awaitTermination() {
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
