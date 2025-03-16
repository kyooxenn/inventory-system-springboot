package com.java.inventory.system.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;

@Entity
@Data
@Builder
public class Product {
    @Id
    private Long id;
    private String name;
    private String description;
    private String productType;
    private int quantity;
    private double unitPrice;

    public Product() {
    }

    public Product(Long id, String name, String description, String productType, int quantity, double unitPrice) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.productType = productType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
}
