package com.davidlab.pricemonitor.price.service;

import com.davidlab.pricemonitor.price.domain.PriceHistory;
import com.davidlab.pricemonitor.price.repository.PriceHistoryRepository;
import com.davidlab.pricemonitor.product.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;

    @Transactional
    public void save(Product product, int price) {
        PriceHistory history = PriceHistory.builder()
                .product(product)
                .price(price)
                .build();
        priceHistoryRepository.save(history);
    }
}
