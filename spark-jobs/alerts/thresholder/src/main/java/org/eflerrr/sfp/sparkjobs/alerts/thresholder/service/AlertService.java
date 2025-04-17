package org.eflerrr.sfp.sparkjobs.alerts.thresholder.service;

import org.apache.spark.broadcast.Broadcast;
import org.eflerrr.sfp.sparkjobs.alerts.thresholder.model.Rule;

import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class AlertService {
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
        Broadcast<Map<String, Rule>> bc = ref.get();
        if (bc == null) {
            return false;
        }
        Rule rule = bc.value().get(deviceId + "#" + metricName);
        if (rule == null) {
            return false;
        }
        return metricValue < rule.lower(ts) || metricValue > rule.upper(ts);
    }
}
