package com.davidlab.pricemonitor.alert.service;

import com.davidlab.pricemonitor.alert.service.dto.AlertDecision;
import com.davidlab.pricemonitor.price.domain.PriceHistory;
import com.davidlab.pricemonitor.price.repository.PriceHistoryRepository;
import com.davidlab.pricemonitor.price.service.PriceHistoryService;
import com.davidlab.pricemonitor.product.domain.Product;
import com.davidlab.pricemonitor.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;

/**
 * Holds the short, write transaction: persists the crawled price and decides
 * whether an alert should be sent. Deliberately contains no network I/O so the
 * DB connection is not held during crawling/notification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceRecordService {

    private final ProductService productService;
    private final PriceHistoryService priceHistoryService;
    private final PriceHistoryRepository priceHistoryRepository;

    // Max allowed deviation from the previous price; beyond this the value is
    // treated as a likely crawling error (wrong selector) and the alert is held.
    @Value("${alert.price-change-threshold:0.70}")
    private double priceChangeThreshold;

    @Transactional
    public AlertDecision recordAndDecide(Long productId, int price, int cooldownMinutes) {
        Product product = productService.findById(productId);

        // Read previous/lowest BEFORE saving the new record.
        int previousPrice = priceHistoryRepository.findTopByProductOrderByCreatedAtDesc(product)
                .map(PriceHistory::getPrice)
                .orElse(0);
        int lowestBefore = priceHistoryRepository.findTopByProductOrderByPriceAsc(product)
                .map(PriceHistory::getPrice)
                .orElse(Integer.MAX_VALUE);

        // Abnormal change guard: record the value for trend continuity but hold the
        // alert. The next cycle re-evaluates against this value, so a genuine sharp
        // drop is alerted one cycle later, while a one-off scraping glitch is suppressed.
        if (isAbnormalChange(previousPrice, price)) {
            log.warn("Abnormal price change for product [{}]: {} -> {} (> {}%); holding alert",
                    productId, previousPrice, price, Math.round(priceChangeThreshold * 100));
            priceHistoryService.save(product, price);
            return AlertDecision.builder()
                    .shouldNotify(false)
                    .previousPrice(previousPrice)
                    .lowestEver(false)
                    .targetChannels(new HashSet<>(product.getNotificationChannels()))
                    .build();
        }

        priceHistoryService.save(product, price);

        LocalDateTime now = LocalDateTime.now();
        boolean belowTarget = price <= product.getTargetPrice();

        // Notify when below target AND either this is a fresh transition into the
        // target zone (price rose above target then dropped again) or the cooldown elapsed.
        boolean shouldNotify = belowTarget
                && (!product.isBelowTarget() || product.canNotify(now, cooldownMinutes));
        boolean lowestEver = price < lowestBefore;

        if (shouldNotify) {
            product.updateLastNotifiedAt(now);
        }
        product.updateBelowTarget(belowTarget);

        // Copy the LAZY channel collection while still inside the transaction.
        return AlertDecision.builder()
                .shouldNotify(shouldNotify)
                .previousPrice(previousPrice)
                .lowestEver(lowestEver)
                .targetChannels(new HashSet<>(product.getNotificationChannels()))
                .build();
    }

    private boolean isAbnormalChange(int previousPrice, int currentPrice) {
        if (previousPrice <= 0) {
            return false;  // No baseline yet (first record) — cannot judge.
        }
        double changeRate = Math.abs((double) (currentPrice - previousPrice)) / previousPrice;
        return changeRate > priceChangeThreshold;
    }
}
