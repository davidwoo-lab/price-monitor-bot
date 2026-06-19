package com.davidlab.pricemonitor.crawler.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CrawlResult {

    private final String productName;
    private final int price;
    private final String platform;
    private final String url;
    private final boolean success;
    private final String errorMessage;

    public static CrawlResult failure(String url, String platform, String errorMessage) {
        return CrawlResult.builder()
                .url(url)
                .platform(platform)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
