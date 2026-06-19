package com.davidlab.pricemonitor.crawler;

import com.davidlab.pricemonitor.crawler.dto.CrawlResult;

public interface PriceCrawler {

    CrawlResult crawl(String url);

    boolean supports(String url);
}
