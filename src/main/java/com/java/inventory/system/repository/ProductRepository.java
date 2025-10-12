package com.java.inventory.system.repository;


import com.java.inventory.system.model.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // No additional methods are needed as JpaRepository already provides common operations like
    // save(), findById(), findAll(), deleteById(), etc.

    Optional<Product> findById(String id);

    boolean existsById(String id);

    void deleteById(String id);

    Optional<Product> findByItemName(String productName);

    @Query("SELECT p FROM Product p WHERE LOWER(p.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))")
    List<Product> findByItemNameLike(@Param("itemName") String itemName);
}
