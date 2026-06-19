package com.davidlab.pricemonitor.product.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    private Product buildProduct() {
        return Product.builder()
                .name("Test Product")
                .url("https://search.shopping.naver.com/catalog/12345")
                .targetPrice(100000)
                .build();
    }

    @Test
    void canNotify_whenLastNotifiedAtIsNull_returnsTrue() {
        Product product = buildProduct();
        assertThat(product.canNotify(LocalDateTime.now(), 60)).isTrue();
    }

    @Test
    void canNotify_whenCooldownElapsed_returnsTrue() {
        Product product = buildProduct();
        LocalDateTime lastNotified = LocalDateTime.now().minusMinutes(61);
        product.updateLastNotifiedAt(lastNotified);

        assertThat(product.canNotify(LocalDateTime.now(), 60)).isTrue();
    }

    @Test
    void canNotify_whenWithinCooldown_returnsFalse() {
        Product product = buildProduct();
        LocalDateTime lastNotified = LocalDateTime.now().minusMinutes(30);
        product.updateLastNotifiedAt(lastNotified);

        assertThat(product.canNotify(LocalDateTime.now(), 60)).isFalse();
    }

    @Test
    void canNotify_whenExactlyAtCooldownBoundary_returnsFalse() {
        Product product = buildProduct();
        LocalDateTime now = LocalDateTime.now();
        product.updateLastNotifiedAt(now.minusMinutes(60));

        // plusMinutes(60).isBefore(now) — exactly at boundary is NOT before, so false
        assertThat(product.canNotify(now, 60)).isFalse();
    }

    @Test
    void canNotify_afterPriceRoseAndDroppedAgain_returnsTrue() {
        Product product = buildProduct();
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        product.updateLastNotifiedAt(twoHoursAgo);

        assertThat(product.canNotify(LocalDateTime.now(), 60)).isTrue();
    }

    @Test
    void deactivate_setsActiveFalse() {
        Product product = buildProduct();
        assertThat(product.isActive()).isTrue();

        product.deactivate();

        assertThat(product.isActive()).isFalse();
    }
}
