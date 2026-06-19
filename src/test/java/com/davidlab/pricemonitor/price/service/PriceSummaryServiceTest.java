package com.davidlab.pricemonitor.price.service;

import com.davidlab.pricemonitor.price.domain.PriceDailySummary;
import com.davidlab.pricemonitor.price.repository.PriceDailySummaryRepository;
import com.davidlab.pricemonitor.price.repository.PriceHistoryRepository;
import com.davidlab.pricemonitor.product.domain.Product;
import com.davidlab.pricemonitor.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceSummaryServiceTest {

    @Mock PriceHistoryRepository priceHistoryRepository;
    @Mock PriceDailySummaryRepository summaryRepository;
    @Mock ProductRepository productRepository;

    @InjectMocks
    PriceSummaryService priceSummaryService;

    @Test
    void aggregateDaily_whenNoExistingSummary_savesNewSummary() {
        LocalDate date = LocalDate.of(2026, 6, 18);
        Product product = mock(Product.class);
        // [productId, min, max, avg, count]
        Object[] row = {1L, 90000, 110000, 99500.0, 24L};
        when(priceHistoryRepository.aggregateDailyBetween(any(), any())).thenReturn(List.<Object[]>of(row));
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(summaryRepository.findByProductAndSummaryDate(product, date)).thenReturn(Optional.empty());

        int count = priceSummaryService.aggregateDaily(date);

        assertThat(count).isEqualTo(1);
        ArgumentCaptor<PriceDailySummary> captor = ArgumentCaptor.forClass(PriceDailySummary.class);
        verify(summaryRepository).save(captor.capture());
        PriceDailySummary saved = captor.getValue();
        assertThat(saved.getMinPrice()).isEqualTo(90000);
        assertThat(saved.getMaxPrice()).isEqualTo(110000);
        assertThat(saved.getAvgPrice()).isEqualTo(99500);   // rounded
        assertThat(saved.getSampleCount()).isEqualTo(24);
        assertThat(saved.getSummaryDate()).isEqualTo(date);
    }

    @Test
    void aggregateDaily_whenSummaryExists_updatesInsteadOfInserting() {
        LocalDate date = LocalDate.of(2026, 6, 18);
        Product product = mock(Product.class);
        PriceDailySummary existing = mock(PriceDailySummary.class);
        Object[] row = {1L, 90000, 110000, 100000.4, 24L};
        when(priceHistoryRepository.aggregateDailyBetween(any(), any())).thenReturn(List.<Object[]>of(row));
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(summaryRepository.findByProductAndSummaryDate(product, date)).thenReturn(Optional.of(existing));

        priceSummaryService.aggregateDaily(date);

        verify(existing).update(90000, 110000, 100000, 24);   // 100000.4 rounds down
        verify(summaryRepository, never()).save(any());
    }

    @Test
    void purgeHistoryBefore_deletesByCutoff() {
        LocalDate cutoff = LocalDate.of(2026, 5, 20);
        when(priceHistoryRepository.deleteByCreatedAtBefore(any())).thenReturn(150);

        int deleted = priceSummaryService.purgeHistoryBefore(cutoff);

        assertThat(deleted).isEqualTo(150);
        verify(priceHistoryRepository).deleteByCreatedAtBefore(eq(LocalDateTime.of(2026, 5, 20, 0, 0)));
    }
}
