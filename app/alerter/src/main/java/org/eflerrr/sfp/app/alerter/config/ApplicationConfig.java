package org.eflerrr.sfp.app.alerter.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record ApplicationConfig(
        @NotEmpty
        String telegramToken,
        @NotNull
        Kafka kafka
) {
    public record Kafka(
            @NotEmpty
            String bootstrapServers,
            @NotEmpty
            String topic,
            @NotEmpty
            String clientId,
            @NotEmpty
            String groupId,
            @NotEmpty
            String autoOffsetReset
    ) {
    }
}
