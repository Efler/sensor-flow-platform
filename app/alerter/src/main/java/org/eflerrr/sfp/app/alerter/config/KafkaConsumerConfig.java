package org.eflerrr.sfp.app.alerter.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eflerrr.sfp.app.alerter.model.Alert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    private final ApplicationConfig config;

    @Autowired
    public KafkaConsumerConfig(ApplicationConfig config) {
        this.config = config;
    }

    @Bean
    public ConsumerFactory<String, Alert> consumerFactory() {
        var jsonDeserializer = new JsonDeserializer<>(Alert.class);
        jsonDeserializer.configure(new HashMap<>() {{
            put(JsonDeserializer.TRUSTED_PACKAGES, "*");
            put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        }}, false);
        return new DefaultKafkaConsumerFactory<>(new HashMap<>() {
            {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.kafka().bootstrapServers());
                put(ConsumerConfig.CLIENT_ID_CONFIG, config.kafka().clientId());
                put(ConsumerConfig.GROUP_ID_CONFIG, config.kafka().groupId());
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.kafka().autoOffsetReset());
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
                put(SaslConfigs.SASL_MECHANISM, "PLAIN");
                put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                                                  "username=\"admin\" password=\"admin-pass\";");
                put("security.protocol", "SASL_PLAINTEXT");
            }
        }, new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Alert>
    kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Alert>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

}
