package com.davidlab.pricemonitor.price.repository;

import com.davidlab.pricemonitor.price.domain.PriceDailySummary;
import com.davidlab.pricemonitor.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface PriceDailySummaryRepository extends JpaRepository<PriceDailySummary, Long> {

    Optional<PriceDailySummary> findByProductAndSummaryDate(Product product, LocalDate summaryDate);
}
