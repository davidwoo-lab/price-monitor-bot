package com.davidlab.pricemonitor.crawler;

import com.davidlab.pricemonitor.crawler.dto.CrawlResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;

/**
 * Shared Jsoup crawling skeleton: handles fetching, retry on transient failures,
 * and price validation. Concrete crawlers only provide selectors and platform info.
 */
@Slf4j
public abstract class AbstractJsoupCrawler implements PriceCrawler {

    protected static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    protected static final int TIMEOUT_MS = 5000;

    @Value("${crawler.max-retry-count:2}")
    private int maxRetryCount;

    @Value("${crawler.retry-delay-ms:1000}")
    private long retryDelayMs;

    protected abstract String platform();

    protected abstract String extractProductName(Document doc);

    protected abstract int extractPrice(Document doc);

    protected Document fetchDocument(String url) throws Exception {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();
    }

    @Override
    public CrawlResult crawl(String url) {
        int totalAttempts = maxRetryCount + 1;
        Exception lastError = null;

        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                Document doc = fetchDocument(url);
                String productName = extractProductName(doc);
                int price = extractPrice(doc);
                return CrawlResult.builder()
                        .productName(productName)
                        .price(price)
                        .platform(platform())
                        .url(url)
                        .success(true)
                        .build();
            } catch (Exception e) {
                lastError = e;
                log.warn("Crawl attempt {}/{} failed for {} [{}]: {}",
                        attempt, totalAttempts, platform(), url, e.getMessage());
                if (attempt < totalAttempts) {
                    sleepBeforeRetry();
                }
            }
        }

        String message = lastError != null ? lastError.getMessage() : "unknown error";
        log.error("Failed to crawl {} URL after {} attempts: {}", platform(), totalAttempts, url, lastError);
        return CrawlResult.failure(url, platform(), message);
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Strips non-digits and validates the result. Empty or non-positive prices are
     * treated as crawl failures (out-of-stock / hidden price) to avoid false alerts.
     */
    protected int parsePrice(String text) {
        String priceText = text.replaceAll("[^0-9]", "");
        if (priceText.isEmpty()) {
            throw new IllegalStateException("Price text is empty after parsing (possibly out of stock)");
        }
        int price = Integer.parseInt(priceText);
        if (price <= 0) {
            throw new IllegalStateException("Invalid price (<= 0), possibly out of stock: " + text);
        }
        return price;
    }
}
