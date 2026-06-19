package com.davidlab.pricemonitor.notification.kakao;

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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.kakao.enabled", havingValue = "true")
public class KakaoNotifier implements NotificationChannel {

    private static final String KAKAO_NOTIFY_URL = "https://notify-api.kakao.com/v1/notify";

    private final String accessToken;
    private final RestTemplate restTemplate;

    public KakaoNotifier(
            @Value("${notification.kakao.access-token}") String accessToken,
            RestTemplate restTemplate) {
        this.accessToken = accessToken;
        this.restTemplate = restTemplate;
    }

    @Override
    public void send(NotificationMessage message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(accessToken);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("message", message.format());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(KAKAO_NOTIFY_URL, request, String.class);
        log.info("KakaoTalk notification sent: {}", message.getProductName());
    }

    @Override
    public ChannelType getType() {
        return ChannelType.KAKAO;
    }
}
