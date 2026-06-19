package com.davidlab.pricemonitor.notification.slack;

import com.davidlab.pricemonitor.notification.ChannelType;
import com.davidlab.pricemonitor.notification.NotificationChannel;
import com.davidlab.pricemonitor.notification.dto.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.slack.enabled", havingValue = "true")
public class SlackNotifier implements NotificationChannel {

    private final String webhookUrl;
    private final RestTemplate restTemplate;

    public SlackNotifier(
            @Value("${notification.slack.webhook-url}") String webhookUrl,
            RestTemplate restTemplate) {
        this.webhookUrl = webhookUrl;
        this.restTemplate = restTemplate;
    }

    @Override
    public void send(NotificationMessage message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("text", message.format());
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(webhookUrl, request, String.class);
        log.info("Slack notification sent: {}", message.getProductName());
    }

    @Override
    public ChannelType getType() {
        return ChannelType.SLACK;
    }
}
