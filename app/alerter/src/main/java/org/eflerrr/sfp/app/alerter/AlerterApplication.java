package org.eflerrr.sfp.app.alerter;

import org.eflerrr.sfp.app.alerter.config.ApplicationConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationConfig.class)
public class AlerterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlerterApplication.class, args);
    }

}
