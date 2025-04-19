package org.eflerrr.sfp.app.alerter.config;

import com.pengrad.telegrambot.TelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BotConfig {
    private final ApplicationConfig config;

    @Autowired
    public BotConfig(ApplicationConfig config) {
        this.config = config;
    }

    @Bean
    public TelegramBot telegramBotBean() {
        return new TelegramBot(config.telegramToken());
    }
}
