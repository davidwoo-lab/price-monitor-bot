package com.davidlab.pricemonitor.notification.telegram;

import com.davidlab.pricemonitor.notification.ChannelType;
import com.davidlab.pricemonitor.notification.NotificationChannel;
import com.davidlab.pricemonitor.notification.dto.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.telegram.enabled", havingValue = "true")
public class TelegramNotifier implements NotificationChannel {

    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";

    private final String botToken;
    private final String chatId;
    private final RestTemplate restTemplate;

    public TelegramNotifier(
            @Value("${notification.telegram.bot-token}") String botToken,
            @Value("${notification.telegram.chat-id}") String chatId,
            RestTemplate restTemplate) {
        this.botToken = botToken;
        this.chatId = chatId;
        this.restTemplate = restTemplate;
    }

    @Override
    public void send(NotificationMessage message) {
        String url = UriComponentsBuilder
                .fromUriString(TELEGRAM_API_BASE + botToken + "/sendMessage")
                .queryParam("chat_id", chatId)
                .queryParam("text", message.format())
                .build()
                .toUriString();

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        log.info("Telegram notification sent: {}, status: {}", message.getProductName(), response.getStatusCode());
    }

    @Override
    public ChannelType getType() {
        return ChannelType.TELEGRAM;
    }
}
