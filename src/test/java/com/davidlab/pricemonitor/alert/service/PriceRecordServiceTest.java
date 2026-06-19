package com.davidlab.pricemonitor.alert.service;

import com.davidlab.pricemonitor.alert.service.dto.AlertDecision;
import com.davidlab.pricemonitor.notification.ChannelType;
import com.davidlab.pricemonitor.price.domain.PriceHistory;
import com.davidlab.pricemonitor.price.repository.PriceHistoryRepository;
import com.davidlab.pricemonitor.price.service.PriceHistoryService;
import com.davidlab.pricemonitor.product.domain.Product;
import com.davidlab.pricemonitor.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceRecordServiceTest {

    @Mock ProductService productService;
    @Mock PriceHistoryService priceHistoryService;
    @Mock PriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    PriceRecordService priceRecordService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(priceRecordService, "priceChangeThreshold", 0.70);
    }

    private Product buildProduct(int targetPrice) {
        return Product.builder()
                .name("Test Product")
                .url("https://search.shopping.naver.com/catalog/12345")
                .targetPrice(targetPrice)
                .build();
    }

    private PriceHistory history(int price) {
        return PriceHistory.builder().price(price).build();
    }

    /** Stubs findById and the previous/lowest lookups. */
    private void stubLookups(Product product, Integer previousPrice, Integer lowestPrice) {
        when(productService.findById(any())).thenReturn(product);
        when(priceHistoryRepository.findTopByProductOrderByCreatedAtDesc(any()))
                .thenReturn(previousPrice == null ? Optional.empty() : Optional.of(history(previousPrice)));
        when(priceHistoryRepository.findTopByProductOrderByPriceAsc(any()))
                .thenReturn(lowestPrice == null ? Optional.empty() : Optional.of(history(lowestPrice)));
    }

    @Test
    void recordAndDecide_whenFreshlyBelowTarget_notifiesAndSavesHistory() {
        Product product = buildProduct(100000);
        product.assignChannel(ChannelType.SLACK);
        stubLookups(product, null, null);

        AlertDecision decision = priceRecordService.recordAndDecide(1L, 90000, 60);

        assertThat(decision.isShouldNotify()).isTrue();
        assertThat(decision.getTargetChannels()).containsExactly(ChannelType.SLACK);
        assertThat(product.isBelowTarget()).isTrue();
        assertThat(product.getLastNotifiedAt()).isNotNull();
        verify(priceHistoryService, times(1)).save(product, 90000);
    }

    @Test
    void assignChannel_enforcesSingleChannel() {
        Product product = buildProduct(100000);
        product.assignChannel(ChannelType.SLACK);
        product.assignChannel(ChannelType.TELEGRAM);

        assertThat(product.getNotificationChannels()).containsExactly(ChannelType.TELEGRAM);
    }

    @Test
    void recordAndDecide_whenAboveTarget_doesNotNotify() {
        Product product = buildProduct(100000);
        stubLookups(product, null, null);

        AlertDecision decision = priceRecordService.recordAndDecide(1L, 110000, 60);

        assertThat(decision.isShouldNotify()).isFalse();
        assertThat(product.isBelowTarget()).isFalse();
        assertThat(product.getLastNotifiedAt()).isNull();
    }

    @Test
    void recordAndDecide_whenStillBelowWithinCooldown_doesNotNotify() {
        Product product = buildProduct(100000);
        product.updateBelowTarget(true);
        product.updateLastNotifiedAt(LocalDateTime.now().minusMinutes(30));
        stubLookups(product, 95000, 95000);

        AlertDecision decision = priceRecordService.recordAndDecide(1L, 90000, 60);

        assertThat(decision.isShouldNotify()).isFalse();
    }

    @Test
    void recordAndDecide_whenStillBelowAndCooldownElapsed_notifies() {
        Product product = buildProduct(100000);
        product.updateBelowTarget(true);
        product.updateLastNotifiedAt(LocalDateTime.now().minusMinutes(61));
        stubLookups(product, 95000, 95000);

        AlertDecision decision = priceRecordService.recordAndDecide(1L, 90000, 60);

        assertThat(decision.isShouldNotify()).isTrue();
    }

    @Test
    void recordAndDecide_whenRoseAboveThenDroppedAgain_notifiesEvenWithinCooldown() {
        Product product = buildProduct(100000);
        // Previously rose above target (belowTarget reset to false) but notified recently.
        product.updateBelowTarget(false);
        product.updateLastNotifiedAt(LocalDateTime.now().minusMinutes(10));
        stubLookups(product, 120000, 95000);

        AlertDecision decision = priceRecordService.recordAndDecide(1L, 90000, 60);

        assertThat(decision.isShouldNotify()).isTrue();
    }

    @Test
    void recordAndDecide_reportsPreviousPriceAndAllTimeLow() {
        Product product = buildProduct(100000);
        stubLookups(product, 110000, 95000);

        AlertDecision decision = priceRecordService.recordAndDecide(1L, 90000, 60);

        assertThat(decision.getPreviousPrice()).isEqualTo(110000);
        assertThat(decision.isLowestEver()).isTrue();
    }

    @Test
    void recordAndDecide_notLowestWhenHigherThanPreviousLow() {
        Product product = buildProduct(100000);
        stubLookups(product, 95000, 90000);

        AlertDecision decision = priceRecordService.recordAndDecide(1L, 95000, 60);

        assertThat(decision.isLowestEver()).isFalse();
    }

    @Test
    void recordAndDecide_whenAbnormalChange_holdsAlertButRecordsHistory() {
        Product product = buildProduct(100000);
        product.assignChannel(ChannelType.SLACK);
        stubLookups(product, 100000, 100000);  // previous price 100,000

        // 90% drop exceeds the 70% threshold → treated as a scraping glitch.
        AlertDecision decision = priceRecordService.recordAndDecide(1L, 10000, 60);

        assertThat(decision.isShouldNotify()).isFalse();
        assertThat(product.getLastNotifiedAt()).isNull();
        assertThat(product.isBelowTarget()).isFalse();   // state not corrupted by the glitch
        verify(priceHistoryService, times(1)).save(product, 10000);  // but trend is recorded
    }
}
