package com.davidlab.pricemonitor.crawler;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class NaverShoppingCrawler extends AbstractJsoupCrawler {

    private static final String PLATFORM = "NAVER";

    @Override
    protected String platform() {
        return PLATFORM;
    }

    @Override
    public boolean supports(String url) {
        return url != null && url.contains("search.shopping.naver.com");
    }

    @Override
    protected String extractProductName(Document doc) {
        Element nameElement = doc.selectFirst("div.prod_info > p.prod_name");
        if (nameElement == null) {
            nameElement = doc.selectFirst("h2.prod_name");
        }
        return nameElement != null ? nameElement.text().trim() : "Unknown";
    }

    @Override
    protected int extractPrice(Document doc) {
        // Naver Shopping lowest price selector — may require adjustment if HTML structure changes
        Element priceElement = doc.selectFirst("span.price_num");
        if (priceElement == null) {
            priceElement = doc.selectFirst("em.num");
        }
        if (priceElement == null) {
            throw new IllegalStateException("Price element not found in Naver Shopping page");
        }
        return parsePrice(priceElement.text());
    }
}
