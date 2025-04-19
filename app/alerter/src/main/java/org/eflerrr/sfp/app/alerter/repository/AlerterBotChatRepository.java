package org.eflerrr.sfp.app.alerter.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AlerterBotChatRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AlerterBotChatRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Long> getChatIds() {
        return jdbcTemplate.query("""
                        SELECT chat_id FROM alerter_bot_chat;
                        """,
                (rs, rowNum) -> rs.getLong("chat_id"));
    }

    public void addChat(Long chatId, String username) {
        jdbcTemplate.update("""
                INSERT INTO alerter_bot_chat (chat_id, username) VALUES (?, ?)
                ON CONFLICT DO NOTHING;
                """, chatId, username);
    }

}
