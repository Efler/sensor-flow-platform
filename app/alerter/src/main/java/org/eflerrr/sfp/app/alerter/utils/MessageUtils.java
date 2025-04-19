package org.eflerrr.sfp.app.alerter.utils;

import org.eflerrr.sfp.app.alerter.model.Alert;

import java.util.regex.Pattern;

public final class MessageUtils {
    private MessageUtils() {
    }

    public static String alertToMessage(Alert alert) {
        return """
                *🚨 ALERT \\(%s\\)*
                
                *Device:* `%s`
                *Metric:* `%s`
                *Value:* `%s`
                *Timestamp:* `%s`
                *Thresholds:*
                • Lower: `%s`
                • Upper: `%s`
                """
                .formatted(
                        markdownSafe(alert.alertType().name()),
                        markdownSafe(alert.deviceId()),
                        markdownSafe(alert.metricName()),
                        alert.metricValue(),
                        alert.srcTimestamp(),
                        alert.thresholds().lower(),
                        alert.thresholds().upper()
                );
    }

    public static String markdownSafe(String str) {
        String specialChars = "_*[]()~`><#+-=|{}.!";
        String regex = "([" + Pattern.quote(specialChars) + "])";
        return str.replaceAll(regex, "\\\\$1");
    }
}
