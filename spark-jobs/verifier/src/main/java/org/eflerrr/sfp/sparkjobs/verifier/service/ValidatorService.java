package org.eflerrr.sfp.sparkjobs.verifier.service;

import org.apache.spark.sql.api.java.UDF6;
import org.eflerrr.sfp.sparkjobs.verifier.utils.HMACUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValidatorService {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorService.class);

    public static UDF6<String, String, Double, Timestamp, String, String, Boolean> getUDF() {
        return ValidatorService::validateData;
    }

    private static Boolean validateData(
            String deviceId, String metricName, Double metricValue, Timestamp srcTimestamp, String signature, String secret)
            throws Exception {
        var MDC = String.format(
                "[deviceId=%s, metricName=%s, metricValue=%s, srcTimestamp=%s]",
                deviceId, metricName, metricValue, srcTimestamp);
        logger.info("Validating data.. : {}", MDC);
        if (deviceId == null || metricName == null || metricValue == null || srcTimestamp == null || signature == null) {
            logger.error("Incomplete data! : {}", MDC);
            return false;
        }
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
