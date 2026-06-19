package com.davidlab.pricemonitor.crawler;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class CoupangCrawler extends AbstractJsoupCrawler {

    private static final String PLATFORM = "COUPANG";

    // TODO: Coupang uses Cloudflare + JS rendering, which Jsoup cannot bypass.
    //       If price extraction fails consistently, consider switching to:
    //       1. Coupang Partners API (recommended) — requires partner account registration
    //       2. Playwright/Selenium — headless browser with full JS rendering support

    @Override
    protected String platform() {
        return PLATFORM;
    }

    @Override
    public boolean supports(String url) {
        return url != null && url.contains("coupang.com");
    }

    @Override
    protected String extractProductName(Document doc) {
        Element nameElement = doc.selectFirst("h2.prod-buy-header__title");
        if (nameElement == null) {
            nameElement = doc.selectFirst("h1.prod-buy-header__title");
        }
        return nameElement != null ? nameElement.text().trim() : "Unknown";
    }

    @Override
    protected int extractPrice(Document doc) {
        // Coupang price selector — high chance of failure due to Cloudflare/JS rendering
        Element priceElement = doc.selectFirst("span.total-price > strong");
        if (priceElement == null) {
            priceElement = doc.selectFirst("span.price-value");
        }
        if (priceElement == null) {
            throw new IllegalStateException("Price element not found in Coupang page — possible JS rendering block");
        }
        return parsePrice(priceElement.text());
    }
}
