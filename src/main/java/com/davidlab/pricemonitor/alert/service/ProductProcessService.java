package com.davidlab.pricemonitor.alert.service;

import com.davidlab.pricemonitor.alert.service.dto.AlertDecision;
import com.davidlab.pricemonitor.crawler.CrawlerFactory;
import com.davidlab.pricemonitor.crawler.PriceCrawler;
import com.davidlab.pricemonitor.crawler.dto.CrawlResult;
import com.davidlab.pricemonitor.notification.NotificationDispatcher;
import com.davidlab.pricemonitor.notification.dto.NotificationMessage;
import com.davidlab.pricemonitor.product.domain.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates per-product processing. Crawling (HTTP I/O) and notification
 * dispatch run outside any transaction; only {@link PriceRecordService} touches
 * the DB within a short transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductProcessService {

    private final CrawlerFactory crawlerFactory;
    private final PriceRecordService priceRecordService;
    private final NotificationDispatcher notificationDispatcher;

    public boolean processProduct(Product product, int notificationCooldownMinutes) {
        // 1) Crawl outside any transaction (network I/O must not hold a DB connection).
        PriceCrawler crawler = crawlerFactory.getCrawler(product.getUrl());
        CrawlResult result = crawler.crawl(product.getUrl());

        if (!result.isSuccess()) {
            log.warn("Crawl failed for product [{}]: {}", product.getId(), result.getErrorMessage());
            return false;
        }

        // 2) Persist + decide within a short transaction.
        AlertDecision decision = priceRecordService.recordAndDecide(
                product.getId(), result.getPrice(), notificationCooldownMinutes);

        // 3) Dispatch outside the transaction.
        if (decision.isShouldNotify()) {
            sendAlert(product, result, decision);
        } else {
            log.debug("No alert for product [{}] at price {}", product.getId(), result.getPrice());
        }
        return true;
    }

    private void sendAlert(Product product, CrawlResult result, AlertDecision decision) {
        NotificationMessage message = NotificationMessage.builder()
                .productName(product.getName())
                .platform(result.getPlatform())
                .currentPrice(result.getPrice())
                .targetPrice(product.getTargetPrice())
                .url(product.getUrl())
                .previousPrice(decision.getPreviousPrice())
                .lowestEver(decision.isLowestEver())
                .build();
        notificationDispatcher.dispatch(message, decision.getTargetChannels());
        log.info("Alert dispatched for product [{}] at price {} via {}",
                product.getId(), result.getPrice(), decision.getTargetChannels());
    }
}
