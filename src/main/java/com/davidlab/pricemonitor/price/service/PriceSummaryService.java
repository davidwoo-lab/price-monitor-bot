package com.davidlab.pricemonitor.price.service;

import com.davidlab.pricemonitor.price.domain.PriceDailySummary;
import com.davidlab.pricemonitor.price.repository.PriceDailySummaryRepository;
import com.davidlab.pricemonitor.price.repository.PriceHistoryRepository;
import com.davidlab.pricemonitor.product.domain.Product;
import com.davidlab.pricemonitor.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceSummaryService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final PriceDailySummaryRepository summaryRepository;
    private final ProductRepository productRepository;

    /**
     * Aggregates one day's price_history into per-product daily summaries.
     * Idempotent: re-running the same date updates existing rows.
     */
    @Transactional
    public int aggregateDaily(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Object[]> rows = priceHistoryRepository.aggregateDailyBetween(start, end);
        for (Object[] row : rows) {
            Long productId = (Long) row[0];
            int minPrice = (Integer) row[1];
            int maxPrice = (Integer) row[2];
            int avgPrice = (int) Math.round((Double) row[3]);
            int sampleCount = ((Long) row[4]).intValue();

            Product product = productRepository.getReferenceById(productId);
            summaryRepository.findByProductAndSummaryDate(product, date).ifPresentOrElse(
                    existing -> existing.update(minPrice, maxPrice, avgPrice, sampleCount),
                    () -> summaryRepository.save(PriceDailySummary.builder()
                            .product(product)
                            .summaryDate(date)
                            .minPrice(minPrice)
                            .maxPrice(maxPrice)
                            .avgPrice(avgPrice)
                            .sampleCount(sampleCount)
                            .build())
            );
        }
        log.info("Aggregated daily price summary for {}: {} product(s)", date, rows.size());
        return rows.size();
    }

    /**
     * Deletes raw price_history older than the cutoff date (already aggregated).
     */
    @Transactional
    public int purgeHistoryBefore(LocalDate cutoffDate) {
        int deleted = priceHistoryRepository.deleteByCreatedAtBefore(cutoffDate.atStartOfDay());
        log.info("Purged {} price_history row(s) before {}", deleted, cutoffDate);
        return deleted;
    }
}
