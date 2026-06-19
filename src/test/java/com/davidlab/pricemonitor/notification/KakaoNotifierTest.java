package com.davidlab.pricemonitor.notification;

import com.davidlab.pricemonitor.notification.dto.NotificationMessage;
import com.davidlab.pricemonitor.notification.kakao.KakaoNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KakaoNotifierTest {

    @Mock
    RestTemplate restTemplate;

    KakaoNotifier kakaoNotifier;

    private static final String ACCESS_TOKEN = "TEST_KAKAO_ACCESS_TOKEN";
    private static final String KAKAO_NOTIFY_URL = "https://notify-api.kakao.com/v1/notify";

    @BeforeEach
    void setUp() {
        kakaoNotifier = new KakaoNotifier(ACCESS_TOKEN, restTemplate);
    }

    @Test
    void send_postsToKakaoNotifyUrl() {
        when(restTemplate.postForEntity(eq(KAKAO_NOTIFY_URL), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        kakaoNotifier.send(buildMessage());

        verify(restTemplate, times(1)).postForEntity(eq(KAKAO_NOTIFY_URL), any(), eq(String.class));
    }

    @Test
    void send_includesBearerTokenInHeader() {
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        kakaoNotifier.send(buildMessage());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Object>> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(any(String.class), requestCaptor.capture(), eq(String.class));

        String authHeader = requestCaptor.getValue().getHeaders().getFirst("Authorization");
        assertThat(authHeader).isEqualTo("Bearer " + ACCESS_TOKEN);
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
