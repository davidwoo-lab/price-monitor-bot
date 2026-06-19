package com.davidlab.pricemonitor.product.repository;

import com.davidlab.pricemonitor.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByActiveTrue();
}
