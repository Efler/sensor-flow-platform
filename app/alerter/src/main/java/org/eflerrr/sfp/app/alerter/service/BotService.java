package org.eflerrr.sfp.app.alerter.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.eflerrr.sfp.app.alerter.model.Alert;
import org.eflerrr.sfp.app.alerter.model.BotChatsList;
import org.eflerrr.sfp.app.alerter.repository.AlerterBotChatRepository;
import org.eflerrr.sfp.app.alerter.utils.MessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BotService {
    private final TelegramBot bot;
    private final BotChatsList chatsList;
    private final AlerterBotChatRepository chatRepository;

    @Autowired
    public BotService(
            TelegramBot bot,
            BotChatsList chatsList,
            AlerterBotChatRepository chatRepository
    ) {
        this.bot = bot;
        this.chatsList = chatsList;
        this.chatRepository = chatRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startBot() {
        bot.setUpdatesListener(updates -> {
            for (var update : updates) {
                if (update.message().text().equals("/start")) {
                    bot.execute(handleRegistration(update));
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, ex -> {
            if (ex.response() != null) {
                log.error(
                        "Bad response from Telegram! Error code: {}; Response Description: {}",
                        ex.response().errorCode(),
                        ex.response().description()
                );
            } else {
                log.error(
                        "Empty response (probably network error)! Message: {}",
                        ex.getMessage());
            }
        });
    }

    private SendMessage handleRegistration(Update update) {
        var chatId = update.message().chat().id();
        var username = update.message().chat().username();
        chatRepository.addChat(chatId, username);
        chatsList.add(chatId);
        log.info("Chat [id={}] is registered, username={}", chatId, username);
        return new SendMessage(chatId,
                "Привет\\! Чат __зарегистрирован__, теперь сюда будут приходить __алерты__\\!")
                .parseMode(ParseMode.MarkdownV2);
    }

    @KafkaListener(
            topics = "${app.kafka.topic}",
            groupId = "${app.kafka.group-id}}"
    )
    private void handleAlert(Alert alert) {
        log.info("Handling alert!  [{}]", alert);
        for (var chatId : chatsList.getAll()) {
            bot.execute(new SendMessage(chatId, MessageUtils.alertToMessage(alert))
                    .parseMode(ParseMode.MarkdownV2));
        }
    }

}
