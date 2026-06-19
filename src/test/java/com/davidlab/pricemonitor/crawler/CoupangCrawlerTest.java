package com.davidlab.pricemonitor.crawler;

import com.davidlab.pricemonitor.crawler.dto.CrawlResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class CoupangCrawlerTest {

    @Spy
    CoupangCrawler crawler;

    @Test
    void supports_coupangUrl_returnsTrue() {
        assertThat(crawler.supports("https://www.coupang.com/vp/products/12345")).isTrue();
    }

    @Test
    void supports_naverUrl_returnsFalse() {
        assertThat(crawler.supports("https://search.shopping.naver.com/catalog/12345")).isFalse();
    }

    @Test
    void supports_nullUrl_returnsFalse() {
        assertThat(crawler.supports(null)).isFalse();
    }

    @Test
    void crawl_withTotalPriceSelector_returnsSuccessResult() throws Exception {
        String html = "<html><body>"
                + "<h2 class='prod-buy-header__title'>Galaxy S24</h2>"
                + "<span class='total-price'><strong>1,099,000</strong></span>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);
        doReturn(doc).when(crawler).fetchDocument(anyString());

        CrawlResult result = crawler.crawl("https://www.coupang.com/vp/products/12345");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPrice()).isEqualTo(1099000);
        assertThat(result.getPlatform()).isEqualTo("COUPANG");
    }

    @Test
    void crawl_withFallbackPriceValueSelector_returnsSuccessResult() throws Exception {
        String html = "<html><body>"
                + "<h1 class='prod-buy-header__title'>Test Item</h1>"
                + "<span class='price-value'>599,000</span>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);
        doReturn(doc).when(crawler).fetchDocument(anyString());

        CrawlResult result = crawler.crawl("https://www.coupang.com/vp/products/12345");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPrice()).isEqualTo(599000);
    }

    @Test
    void crawl_whenPriceElementMissing_returnsFailureResult() throws Exception {
        String html = "<html><body><h2 class='prod-buy-header__title'>Test</h2></body></html>";
        Document doc = Jsoup.parse(html);
        doReturn(doc).when(crawler).fetchDocument(anyString());

        CrawlResult result = crawler.crawl("https://www.coupang.com/vp/products/12345");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isNotNull();
    }

    @Test
    void crawl_whenFetchDocumentThrows_returnsFailureResult() throws Exception {
        doThrow(new RuntimeException("Cloudflare block")).when(crawler).fetchDocument(anyString());

        CrawlResult result = crawler.crawl("https://www.coupang.com/vp/products/12345");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Cloudflare block");
    }
}
