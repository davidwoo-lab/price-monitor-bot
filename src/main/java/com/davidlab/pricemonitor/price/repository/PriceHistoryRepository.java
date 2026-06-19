package com.davidlab.pricemonitor.price.repository;

import com.davidlab.pricemonitor.price.domain.PriceHistory;
import com.davidlab.pricemonitor.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findTop10ByProductOrderByCreatedAtDesc(Product product);

    // Most recent recorded price (used as "previous price" before saving the new one).
    Optional<PriceHistory> findTopByProductOrderByCreatedAtDesc(Product product);

    // Lowest recorded price ever (used to detect all-time-low).
    Optional<PriceHistory> findTopByProductOrderByPriceAsc(Product product);

    /**
     * Per-product aggregation over a time range. Each row:
     * [productId(Long), min(Integer), max(Integer), avg(Double), count(Long)].
     */
    @Query("SELECT ph.product.id, MIN(ph.price), MAX(ph.price), AVG(ph.price), COUNT(ph) " +
            "FROM PriceHistory ph " +
            "WHERE ph.createdAt >= :start AND ph.createdAt < :end " +
            "GROUP BY ph.product.id")
    List<Object[]> aggregateDailyBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Modifying
    @Query("DELETE FROM PriceHistory ph WHERE ph.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
