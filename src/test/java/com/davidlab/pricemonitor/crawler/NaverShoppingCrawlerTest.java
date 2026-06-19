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
class NaverShoppingCrawlerTest {

    @Spy
    NaverShoppingCrawler crawler;

    @Test
    void supports_naverShoppingUrl_returnsTrue() {
        assertThat(crawler.supports("https://search.shopping.naver.com/catalog/12345")).isTrue();
    }

    @Test
    void supports_coupangUrl_returnsFalse() {
        assertThat(crawler.supports("https://www.coupang.com/vp/products/12345")).isFalse();
    }

    @Test
    void supports_nullUrl_returnsFalse() {
        assertThat(crawler.supports(null)).isFalse();
    }

    @Test
    void crawl_withPriceNumSelector_returnsSuccessResult() throws Exception {
        String html = "<html><body>"
                + "<div class='prod_info'><p class='prod_name'>AirPods Pro</p></div>"
                + "<span class='price_num'>289,000</span>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);
        doReturn(doc).when(crawler).fetchDocument(anyString());

        CrawlResult result = crawler.crawl("https://search.shopping.naver.com/catalog/12345");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPrice()).isEqualTo(289000);
        assertThat(result.getPlatform()).isEqualTo("NAVER");
    }

    @Test
    void crawl_withFallbackEmNumSelector_returnsSuccessResult() throws Exception {
        String html = "<html><body>"
                + "<h2 class='prod_name'>Test Item</h2>"
                + "<em class='num'>150,000</em>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);
        doReturn(doc).when(crawler).fetchDocument(anyString());

        CrawlResult result = crawler.crawl("https://search.shopping.naver.com/catalog/12345");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPrice()).isEqualTo(150000);
    }

    @Test
    void crawl_whenPriceElementMissing_returnsFailureResult() throws Exception {
        String html = "<html><body><h2 class='prod_name'>Test Item</h2></body></html>";
        Document doc = Jsoup.parse(html);
        doReturn(doc).when(crawler).fetchDocument(anyString());

        CrawlResult result = crawler.crawl("https://search.shopping.naver.com/catalog/12345");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isNotNull();
    }

    @Test
    void crawl_whenFetchDocumentThrows_returnsFailureResult() throws Exception {
        doThrow(new RuntimeException("Connection refused")).when(crawler).fetchDocument(anyString());

        CrawlResult result = crawler.crawl("https://search.shopping.naver.com/catalog/12345");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Connection refused");
    }
}
