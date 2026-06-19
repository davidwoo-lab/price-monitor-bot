package com.davidlab.pricemonitor.notification;

import com.davidlab.pricemonitor.notification.dto.NotificationMessage;
import com.davidlab.pricemonitor.notification.slack.SlackNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlackNotifierTest {

    @Mock
    RestTemplate restTemplate;

    SlackNotifier slackNotifier;

    private static final String WEBHOOK_URL = "https://hooks.slack.com/services/TEST/WEBHOOK";

    @BeforeEach
    void setUp() {
        slackNotifier = new SlackNotifier(WEBHOOK_URL, restTemplate);
    }

    @Test
    void send_postsJsonToWebhookUrl() {
        when(restTemplate.postForEntity(eq(WEBHOOK_URL), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        NotificationMessage message = buildMessage();
        slackNotifier.send(message);

        verify(restTemplate, times(1)).postForEntity(eq(WEBHOOK_URL), any(), eq(String.class));
    }

    @Test
    void send_messageContainsProductName() {
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        NotificationMessage message = buildMessage();
        slackNotifier.send(message);

        verify(restTemplate).postForEntity(eq(WEBHOOK_URL), any(), eq(String.class));
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
