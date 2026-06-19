package com.davidlab.pricemonitor.crawler;

import com.davidlab.pricemonitor.common.exception.UnsupportedPlatformException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrawlerFactoryTest {

    private CrawlerFactory crawlerFactory;

    @BeforeEach
    void setUp() {
        List<PriceCrawler> crawlers = List.of(new NaverShoppingCrawler(), new CoupangCrawler());
        crawlerFactory = new CrawlerFactory(crawlers);
    }

    @Test
    void getCrawler_naverUrl_returnsNaverShoppingCrawler() {
        PriceCrawler crawler = crawlerFactory.getCrawler("https://search.shopping.naver.com/catalog/12345");
        assertThat(crawler).isInstanceOf(NaverShoppingCrawler.class);
    }

    @Test
    void getCrawler_coupangUrl_returnsCoupangCrawler() {
        PriceCrawler crawler = crawlerFactory.getCrawler("https://www.coupang.com/vp/products/12345");
        assertThat(crawler).isInstanceOf(CoupangCrawler.class);
    }

    @Test
    void getCrawler_unsupportedUrl_throwsUnsupportedPlatformException() {
        assertThatThrownBy(() -> crawlerFactory.getCrawler("https://www.gmarket.co.kr/item/12345"))
                .isInstanceOf(UnsupportedPlatformException.class);
    }
}
