package org.eflerrr.sfp.sparkjobs.verifier.service;

import org.apache.spark.broadcast.Broadcast;
import org.eflerrr.sfp.sparkjobs.verifier.utils.HMACUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValidatorService implements Serializable {
    private final AtomicReference<Broadcast<Map<String, String>>> secretsRef = new AtomicReference<>();

    public ValidatorService(Broadcast<Map<String, String>> initBc) {
        this.secretsRef.set(initBc);
    }

    public void updateBroadcast(Broadcast<Map<String, String>> bc) {
        var old = secretsRef.getAndSet(bc);
        if (old != null) {
            old.destroy();
        }
    }

    public Boolean validateData(
            String deviceId, String metricName, Double metricValue, Timestamp srcTimestamp, String signature)
            throws Exception {
        var MDC = String.format(
                "[deviceId=%s, metricName=%s, metricValue=%s, srcTimestamp=%s]",
                deviceId, metricName, metricValue, srcTimestamp);
        Logger logger = LoggerFactory.getLogger(ValidatorService.class);
        logger.info("Validating data.. : {}", MDC);
        if (deviceId == null || metricName == null || metricValue == null || srcTimestamp == null || signature == null) {
            logger.error("Incomplete data! : {}", MDC);
            return false;
        }
        var bc = secretsRef.get();
        if (bc == null) {
            logger.error("Device secrets broadcast is null! : {}", MDC);
            return false;
        }
        var secret = bc.value().get(deviceId);
        if (secret == null) {
            logger.error("Device is not registered! : {}", MDC);
            return false;
        }
        String payload = Stream.of(
                        deviceId, metricName, metricValue.toString(), srcTimestamp.toString())
                .sorted()
                .collect(Collectors.joining());
        String computedSignature = HMACUtils.calculateHMAC(payload, secret);
        if (!computedSignature.equals(signature)) {
            logger.error("Signature is invalid! : {}", MDC);
            return false;
        }
        logger.error("Data is successfully validated : {}", MDC);
        return true;
    }
}
