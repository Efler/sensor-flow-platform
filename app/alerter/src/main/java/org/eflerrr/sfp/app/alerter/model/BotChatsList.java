package org.eflerrr.sfp.app.alerter.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BotChatsList {
    private final Set<Long> chats;

    public BotChatsList() {
        this.chats = ConcurrentHashMap.newKeySet();
    }

    public BotChatsList(Collection<Long> chats) {
        this.chats = ConcurrentHashMap.newKeySet();
        this.chats.addAll(chats);
    }

    public void add(Long chatId) {
        chats.add(chatId);
    }

    public Set<Long> getAll() {
        return Collections.unmodifiableSet(chats);
    }

    public boolean contains(Long id) {
        return chats.contains(id);
    }
}
