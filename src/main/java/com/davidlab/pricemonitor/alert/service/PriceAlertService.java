package com.davidlab.pricemonitor.alert.service;

import com.davidlab.pricemonitor.notification.NotificationDispatcher;
import com.davidlab.pricemonitor.notification.dto.NotificationMessage;
import com.davidlab.pricemonitor.product.domain.Product;
import com.davidlab.pricemonitor.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceAlertService {

    private static final int CONSECUTIVE_FAILURE_THRESHOLD = 3;

    private final ProductService productService;
    private final ProductProcessService productProcessService;
    private final NotificationDispatcher notificationDispatcher;

    @Value("${crawler.request-delay-ms:2000}")
    private long requestDelayMs;

    @Value("${alert.notification-cooldown-minutes:60}")
    private int notificationCooldownMinutes;

    public void checkAndNotify() {
        List<Product> products = productService.findAllActive();
        log.info("Starting price check for {} active products", products.size());

        int consecutiveFailures = 0;

        for (Product product : products) {
            try {
                boolean success = productProcessService.processProduct(product, notificationCooldownMinutes);
                if (!success) {
                    consecutiveFailures++;
                    notifyIfTooManyFailures(consecutiveFailures, product);
                } else {
                    consecutiveFailures = 0;
                }
            } catch (Exception e) {
                consecutiveFailures++;
                log.error("Unexpected error processing product [{}]: {}", product.getId(), e.getMessage(), e);
                notifyIfTooManyFailures(consecutiveFailures, product);
            } finally {
                sleepBetweenRequests();
            }
        }

        log.info("Price check completed");
    }

    private void notifyIfTooManyFailures(int consecutiveFailures, Product product) {
        // Alert on reaching the threshold, then again on each multiple of it,
        // so a sustained outage keeps notifying instead of going silent after the first hit.
        if (consecutiveFailures >= CONSECUTIVE_FAILURE_THRESHOLD
                && consecutiveFailures % CONSECUTIVE_FAILURE_THRESHOLD == 0) {
            log.error("Consecutive crawl failures reached {}. Last failed product: [{}]",
                    consecutiveFailures, product.getId());
            NotificationMessage adminAlert = NotificationMessage.adminAlert(
                    String.format("Consecutive crawling failures: %d (last product id: %d)",
                            consecutiveFailures, product.getId()),
                    product.getUrl());
            // Admin alerts go to every enabled channel, regardless of per-product config.
            notificationDispatcher.dispatchToAll(adminAlert);
        }
    }

    private void sleepBetweenRequests() {
        try {
            Thread.sleep(requestDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted between requests");
        }
    }
}
