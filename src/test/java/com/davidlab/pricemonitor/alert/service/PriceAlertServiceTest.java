package com.davidlab.pricemonitor.alert.service;

import com.davidlab.pricemonitor.notification.NotificationDispatcher;
import com.davidlab.pricemonitor.notification.dto.NotificationMessage;
import com.davidlab.pricemonitor.product.domain.Product;
import com.davidlab.pricemonitor.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceAlertServiceTest {

    @Mock ProductService productService;
    @Mock ProductProcessService productProcessService;
    @Mock NotificationDispatcher notificationDispatcher;

    @InjectMocks
    PriceAlertService priceAlertService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(priceAlertService, "requestDelayMs", 0L);
        ReflectionTestUtils.setField(priceAlertService, "notificationCooldownMinutes", 60);
    }

    private Product buildProduct(String url) {
        return Product.builder()
                .name("Test Product")
                .url(url)
                .targetPrice(100000)
                .build();
    }

    @Test
    void checkAndNotify_whenNoActiveProducts_doesNothing() {
        when(productService.findAllActive()).thenReturn(List.of());

        priceAlertService.checkAndNotify();

        verify(productProcessService, never()).processProduct(any(), anyInt());
        verify(notificationDispatcher, never()).dispatchToAll(any());
    }

    @Test
    void checkAndNotify_callsProcessProductForEachActiveProduct() {
        Product p1 = buildProduct("https://search.shopping.naver.com/catalog/1");
        Product p2 = buildProduct("https://search.shopping.naver.com/catalog/2");
        when(productService.findAllActive()).thenReturn(List.of(p1, p2));
        when(productProcessService.processProduct(any(), anyInt())).thenReturn(true);

        priceAlertService.checkAndNotify();

        verify(productProcessService, times(1)).processProduct(eq(p1), eq(60));
        verify(productProcessService, times(1)).processProduct(eq(p2), eq(60));
    }

    @Test
    void checkAndNotify_whenCrawlFails_continuesNextProduct() {
        Product p1 = buildProduct("https://search.shopping.naver.com/catalog/1");
        Product p2 = buildProduct("https://search.shopping.naver.com/catalog/2");
        when(productService.findAllActive()).thenReturn(List.of(p1, p2));
        when(productProcessService.processProduct(eq(p1), anyInt())).thenReturn(false);
        when(productProcessService.processProduct(eq(p2), anyInt())).thenReturn(true);

        priceAlertService.checkAndNotify();

        verify(productProcessService, times(1)).processProduct(eq(p1), anyInt());
        verify(productProcessService, times(1)).processProduct(eq(p2), anyInt());
    }

    @Test
    void checkAndNotify_whenThreeConsecutiveFailures_sendsAdminAlert() {
        Product p1 = buildProduct("https://search.shopping.naver.com/catalog/1");
        Product p2 = buildProduct("https://search.shopping.naver.com/catalog/2");
        Product p3 = buildProduct("https://search.shopping.naver.com/catalog/3");
        when(productService.findAllActive()).thenReturn(List.of(p1, p2, p3));
        when(productProcessService.processProduct(any(), anyInt())).thenReturn(false);

        priceAlertService.checkAndNotify();

        verify(notificationDispatcher, times(1)).dispatchToAll(any(NotificationMessage.class));
    }

    @Test
    void checkAndNotify_whenMoreThanThreeConsecutiveFailures_sendsAdminAlertOnlyOnce() {
        Product p1 = buildProduct("https://search.shopping.naver.com/catalog/1");
        Product p2 = buildProduct("https://search.shopping.naver.com/catalog/2");
        Product p3 = buildProduct("https://search.shopping.naver.com/catalog/3");
        Product p4 = buildProduct("https://search.shopping.naver.com/catalog/4");
        when(productService.findAllActive()).thenReturn(List.of(p1, p2, p3, p4));
        when(productProcessService.processProduct(any(), anyInt())).thenReturn(false);

        priceAlertService.checkAndNotify();

        verify(notificationDispatcher, times(1)).dispatchToAll(any(NotificationMessage.class));
    }

    @Test
    void checkAndNotify_whenSuccessAfterFailures_resetsConsecutiveCount() {
        Product p1 = buildProduct("https://search.shopping.naver.com/catalog/1");
        Product p2 = buildProduct("https://search.shopping.naver.com/catalog/2");
        Product p3 = buildProduct("https://search.shopping.naver.com/catalog/3");
        when(productService.findAllActive()).thenReturn(List.of(p1, p2, p3));
        // 2 failures → success resets → 1 more failure: total only 2 consecutive at most
        when(productProcessService.processProduct(eq(p1), anyInt())).thenReturn(false);
        when(productProcessService.processProduct(eq(p2), anyInt())).thenReturn(true);
        when(productProcessService.processProduct(eq(p3), anyInt())).thenReturn(false);

        priceAlertService.checkAndNotify();

        // never hits 3 consecutive, so no admin alert
        verify(notificationDispatcher, never()).dispatchToAll(any());
    }
}
