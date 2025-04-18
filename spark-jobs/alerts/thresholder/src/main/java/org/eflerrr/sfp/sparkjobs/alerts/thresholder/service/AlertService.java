package org.eflerrr.sfp.sparkjobs.alerts.thresholder.service;

import org.apache.spark.broadcast.Broadcast;
import org.eflerrr.sfp.sparkjobs.alerts.thresholder.model.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.eflerrr.sfp.sparkjobs.alerts.thresholder.model.ThresholdType.LOWER_THRESHOLD_TYPE;
import static org.eflerrr.sfp.sparkjobs.alerts.thresholder.model.ThresholdType.UPPER_THRESHOLD_TYPE;

public class AlertService implements Serializable {
    private final AtomicReference<Broadcast<Map<String, Rule>>> ref = new AtomicReference<>();

    public void updateBroadcast(Broadcast<Map<String, Rule>> bc) {
        Broadcast<Map<String, Rule>> old = ref.getAndSet(bc);
        if (old != null) {
            old.destroy();
        }
    }

    public boolean isAlert(
            String deviceId, String metricName, Double metricValue, Timestamp ts
    ) {
        Logger logger = LoggerFactory.getLogger(AlertService.class);
        var MDC = String.format(
                "[deviceId=%s, metricName=%s, metricValue=%s, srcTimestamp=%s]",
                deviceId, metricName, metricValue, ts);
        Broadcast<Map<String, Rule>> bc = ref.get();
        if (bc == null) {
            return false;
        }
        Rule lowerRule = bc.value().get(deviceId + "#" + metricName + "#" + LOWER_THRESHOLD_TYPE);
        Rule upperRule = bc.value().get(deviceId + "#" + metricName + "#" + UPPER_THRESHOLD_TYPE);
        if (lowerRule == null && upperRule == null) {
            return false;
        }

        boolean lowerPass = true;
        boolean upperPass = true;
        if (lowerRule != null) {
            double lowerThreshold = lowerRule.threshold(ts);
            if (metricValue < lowerThreshold) {
                logger.info("LOWER threshold is BROKEN:  {} < {}, {}", metricValue, lowerThreshold, MDC);
                lowerPass = false;
            } else {
                logger.info("LOWER threshold is OK:  {} >= {}, {}", metricValue, lowerThreshold, MDC);
            }
        }
        if (upperRule != null) {
            double upperThreshold = upperRule.threshold(ts);
            if (metricValue > upperThreshold) {
                logger.info("UPPER threshold is BROKEN:  {} > {}, {}", metricValue, upperThreshold, MDC);
                upperPass = false;
            } else {
                logger.info("UPPER threshold is OK:  {} <= {}, {}", metricValue, upperThreshold, MDC);
            }
        }

        return !(lowerPass && upperPass);
    }
}
