package com.davidlab.pricemonitor.notification;

import com.davidlab.pricemonitor.notification.dto.NotificationMessage;
import com.davidlab.pricemonitor.notification.telegram.TelegramNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramNotifierTest {

    @Mock
    RestTemplate restTemplate;

    TelegramNotifier telegramNotifier;

    private static final String BOT_TOKEN = "123456:TEST_TOKEN";
    private static final String CHAT_ID = "987654321";

    @BeforeEach
    void setUp() {
        telegramNotifier = new TelegramNotifier(BOT_TOKEN, CHAT_ID, restTemplate);
    }

    @Test
    void send_callsTelegramApiWithCorrectUrl() {
        when(restTemplate.getForEntity(any(String.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        telegramNotifier.send(buildMessage());

        verify(restTemplate, times(1)).getForEntity(
                contains("/bot" + BOT_TOKEN + "/sendMessage"),
                eq(String.class)
        );
    }

    @Test
    void send_includesChatIdInUrl() {
        when(restTemplate.getForEntity(any(String.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        telegramNotifier.send(buildMessage());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).getForEntity(urlCaptor.capture(), eq(String.class));
        assertThat(urlCaptor.getValue()).contains("chat_id=" + CHAT_ID);
    }

    private NotificationMessage buildMessage() {
        return NotificationMessage.builder()
                .productName("AirPods Pro")
                .platform("NAVER")
                .currentPrice(280000)
                .targetPrice(300000)
                .url("https://search.shopping.naver.com/catalog/12345")
                .build();
    }
}
