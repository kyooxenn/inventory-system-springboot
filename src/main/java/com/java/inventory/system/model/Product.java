package com.java.inventory.system.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor // ✅ fixes the error
@AllArgsConstructor
public class Product {
    @Id
    private String id;
    private String itemName;
    private String description;
    private String category;
    private BigDecimal unitPrice;
    private int quantity;
    private String unit;
}
