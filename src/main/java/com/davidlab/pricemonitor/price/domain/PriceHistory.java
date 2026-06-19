package com.davidlab.pricemonitor.price.domain;

import com.davidlab.pricemonitor.common.domain.BaseEntity;
import com.davidlab.pricemonitor.product.domain.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "price_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int price;

    @Builder
    public PriceHistory(Product product, int price) {
        this.product = product;
        this.price = price;
    }
}
