package com.java.inventory.system.repository;


import com.java.inventory.system.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // No additional methods are needed as JpaRepository already provides common operations like
    // save(), findById(), findAll(), deleteById(), etc.

    Optional<Product> findByProductName(String productName);

}
