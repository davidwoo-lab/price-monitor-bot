package com.davidlab.pricemonitor.alert.service;

import com.davidlab.pricemonitor.alert.service.dto.AlertDecision;
import com.davidlab.pricemonitor.crawler.CrawlerFactory;
import com.davidlab.pricemonitor.crawler.PriceCrawler;
import com.davidlab.pricemonitor.crawler.dto.CrawlResult;
import com.davidlab.pricemonitor.notification.ChannelType;
import com.davidlab.pricemonitor.notification.NotificationDispatcher;
import com.davidlab.pricemonitor.notification.dto.NotificationMessage;
import com.davidlab.pricemonitor.product.domain.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductProcessServiceTest {

    @Mock CrawlerFactory crawlerFactory;
    @Mock PriceCrawler priceCrawler;
    @Mock PriceRecordService priceRecordService;
    @Mock NotificationDispatcher notificationDispatcher;

    @InjectMocks
    ProductProcessService productProcessService;

    private Product buildProduct() {
        return Product.builder()
                .name("Test Product")
                .url("https://search.shopping.naver.com/catalog/12345")
                .targetPrice(100000)
                .build();
    }

    private CrawlResult successResult(int price) {
        return CrawlResult.builder()
                .productName("Test Product")
                .price(price)
                .platform("NAVER")
                .url("https://search.shopping.naver.com/catalog/12345")
                .success(true)
                .build();
    }

    @Test
    void processProduct_whenShouldNotify_dispatchesAndReturnsTrue() {
        Product product = buildProduct();
        when(crawlerFactory.getCrawler(anyString())).thenReturn(priceCrawler);
        when(priceCrawler.crawl(anyString())).thenReturn(successResult(90000));
        when(priceRecordService.recordAndDecide(any(), anyInt(), anyInt()))
                .thenReturn(AlertDecision.builder().shouldNotify(true).build());

        boolean result = productProcessService.processProduct(product, 60);

        assertThat(result).isTrue();
        verify(notificationDispatcher, times(1)).dispatch(any(NotificationMessage.class), any());
    }

    @Test
    void processProduct_whenShouldNotNotify_doesNotDispatch() {
        Product product = buildProduct();
        when(crawlerFactory.getCrawler(anyString())).thenReturn(priceCrawler);
        when(priceCrawler.crawl(anyString())).thenReturn(successResult(110000));
        when(priceRecordService.recordAndDecide(any(), anyInt(), anyInt()))
                .thenReturn(AlertDecision.builder().shouldNotify(false).build());

        boolean result = productProcessService.processProduct(product, 60);

        assertThat(result).isTrue();
        verify(notificationDispatcher, never()).dispatch(any(), any());
    }

    @Test
    void processProduct_whenCrawlFails_returnsFalseWithoutRecordingOrDispatching() {
        Product product = buildProduct();
        when(crawlerFactory.getCrawler(anyString())).thenReturn(priceCrawler);
        when(priceCrawler.crawl(anyString()))
                .thenReturn(CrawlResult.failure(product.getUrl(), "NAVER", "Connection error"));

        boolean result = productProcessService.processProduct(product, 60);

        assertThat(result).isFalse();
        verify(priceRecordService, never()).recordAndDecide(any(), anyInt(), anyInt());
        verify(notificationDispatcher, never()).dispatch(any(), any());
    }

    @Test
    void processProduct_enrichesMessageWithPreviousPriceAndLowestEver() {
        Product product = buildProduct();
        when(crawlerFactory.getCrawler(anyString())).thenReturn(priceCrawler);
        when(priceCrawler.crawl(anyString())).thenReturn(successResult(90000));
        when(priceRecordService.recordAndDecide(any(), anyInt(), anyInt()))
                .thenReturn(AlertDecision.builder()
                        .shouldNotify(true)
                        .previousPrice(120000)
                        .lowestEver(true)
                        .targetChannels(Set.of(ChannelType.SLACK))
                        .build());

        productProcessService.processProduct(product, 60);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationDispatcher).dispatch(captor.capture(), eq(Set.of(ChannelType.SLACK)));
        NotificationMessage sent = captor.getValue();
        assertThat(sent.getCurrentPrice()).isEqualTo(90000);
        assertThat(sent.getPreviousPrice()).isEqualTo(120000);
        assertThat(sent.isLowestEver()).isTrue();
    }
}
