package com.davidlab.pricemonitor.crawler;

import com.davidlab.pricemonitor.common.exception.UnsupportedPlatformException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CrawlerFactory {

    private final List<PriceCrawler> crawlers;

    public PriceCrawler getCrawler(String url) {
        return crawlers.stream()
                .filter(c -> c.supports(url))
                .findFirst()
                .orElseThrow(() -> new UnsupportedPlatformException(url));
    }
}
