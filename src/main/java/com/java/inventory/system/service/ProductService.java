package com.java.inventory.system.service;

import com.java.inventory.system.dto.ProductRequest;
import com.java.inventory.system.exception.ProductSvcException;
import com.java.inventory.system.exception.errortypes.ProductSvcErrorType;
import com.java.inventory.system.model.Product;
import com.java.inventory.system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.java.inventory.system.exception.errortypes.ProductSvcErrorType.ERR_INVENTORY_MS_NO_PRODUCT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Cache the result of this method — fetch all products.
     * The first call hits the DB, subsequent calls are served from Redis.
     */
    @Cacheable(value = "allProducts")
    public List<Product> getAllProducts() {
        log.info("Fetching all products from database...");
        List<Product> retrievedProducts = productRepository.findAll();
        log.info("Records retrieved successfully.");
        return retrievedProducts;
    }

    /**
     * Fetch products by name (not cached since it's variable per request).
     */
    public List<Product> getProductByProductName(String productName) {
        return productRepository.findByItemNameLike(productName);
    }

    /**
     * Fetch a single product by ID.
     * You can also cache this individually if needed.
     */
    @Cacheable(value = "productById", key = "#id")
    public Product getProductById(String id) {
        log.info("Fetching product ID [{}] from database...", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductSvcException(ERR_INVENTORY_MS_NO_PRODUCT_FOUND));
    }

    /**
     * Create a new product — clear all cache since data changes.
     */
    @CacheEvict(value = {"allProducts", "productById"}, allEntries = true)
    public Product createProduct(ProductRequest request) throws ProductSvcException {
        productRepository.findByItemName(request.getItemName())
                .ifPresent(p -> {
                    throw new ProductSvcException(ProductSvcErrorType.ERR_INVENTORY_MS_PRODUCT_EXIST);
                });

        Product newProduct = Product.builder()
                .id(request.getId())
                .itemName(request.getItemName())
                .description(request.getDescription())
                .category(request.getCategory())
                .unitPrice(request.getUnitPrice())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .build();

        log.info("Creating new product [{}].", request.getItemName());
        Product savedProduct = productRepository.save(newProduct);
        log.info("created successfully!");
        return savedProduct;
    }

    /**
     * Update an existing product — also clear caches.
     */
    @CacheEvict(value = {"allProducts", "productById"}, allEntries = true)
    public Product updateProduct(String id, ProductRequest request) throws ProductSvcException {
        log.info("Fetching product to update...");
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductSvcException(ERR_INVENTORY_MS_NO_PRODUCT_FOUND));

        log.info("Product found! [{}]", product.getItemName());

        String newName = request.getItemName();
        String currentName = product.getItemName();

        // Check if name already exists (when changed)
        if (!currentName.equalsIgnoreCase(newName)) {
            log.info("Validating if the product exists...");
            productRepository.findByItemName(newName)
                    .ifPresent(p -> {
                        throw new ProductSvcException(ProductSvcErrorType.ERR_INVENTORY_MS_PRODUCT_EXIST);
                    });
        }

        // Apply updates
        updateProductFields(product, request);
        Product updated = productRepository.save(product);
        log.info("Updated successfully!");
        return updated;
    }

    private void updateProductFields(Product product, ProductRequest request) {
        product.setItemName(request.getItemName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setQuantity(request.getQuantity());
        product.setUnitPrice(request.getUnitPrice());
        product.setUnit(request.getUnit());
    }

    /**
     * Delete a product — clear all cached products.
     */
    @CacheEvict(value = {"allProducts", "productById"}, allEntries = true)
    @Transactional
    public boolean deleteProduct(String id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            log.info("Product with ID {} deleted successfully.", id);
            return true;
        }
        log.info("Product with ID {} was not found for deletion.", id);
        return false;
    }
}
