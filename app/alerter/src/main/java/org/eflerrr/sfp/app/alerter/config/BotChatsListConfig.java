package org.eflerrr.sfp.app.alerter.config;

import org.eflerrr.sfp.app.alerter.model.BotChatsList;
import org.eflerrr.sfp.app.alerter.repository.AlerterBotChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BotChatsListConfig {

    private final AlerterBotChatRepository chatRepository;

    @Autowired
    public BotChatsListConfig(AlerterBotChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Bean
    public BotChatsList botChatsList() {
        List<Long> chatIds = chatRepository.getChatIds();
        return new BotChatsList(chatIds);
    }

}
