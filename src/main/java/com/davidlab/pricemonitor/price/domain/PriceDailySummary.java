package com.davidlab.pricemonitor.price.domain;

import com.davidlab.pricemonitor.common.domain.BaseEntity;
import com.davidlab.pricemonitor.product.domain.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Daily per-product price aggregation. Populated by a scheduled job from
 * price_history, then the raw history is purged. Kept for long-term statistics.
 */
@Entity
@Table(
        name = "price_daily_summary",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_summary_product_date",
                columnNames = {"product_id", "summary_date"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceDailySummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(nullable = false)
    private int minPrice;

    @Column(nullable = false)
    private int maxPrice;

    @Column(nullable = false)
    private int avgPrice;

    @Column(nullable = false)
    private int sampleCount;

    @Builder
    public PriceDailySummary(Product product, LocalDate summaryDate,
                             int minPrice, int maxPrice, int avgPrice, int sampleCount) {
        this.product = product;
        this.summaryDate = summaryDate;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.avgPrice = avgPrice;
        this.sampleCount = sampleCount;
    }

    /** Re-aggregation update (idempotent re-runs of the same day). */
    public void update(int minPrice, int maxPrice, int avgPrice, int sampleCount) {
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.avgPrice = avgPrice;
        this.sampleCount = sampleCount;
    }
}
