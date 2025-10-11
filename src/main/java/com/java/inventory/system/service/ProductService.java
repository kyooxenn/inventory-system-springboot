package com.java.inventory.system.service;

import com.java.inventory.system.dto.ProductRequest;
import com.java.inventory.system.exception.ProductSvcException;
import com.java.inventory.system.exception.errortypes.ProductSvcErrorType;
import com.java.inventory.system.model.Product;
import com.java.inventory.system.repository.ProductRepository;
import com.java.inventory.system.util.ProductIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
        return productRepository.findAll();
    }

    /**
     * Fetch products by name (not cached since it's variable per request).
     */
    public List<Product> getProductByProductName(String productName) {
        return productRepository.findByProductNameLike(productName);
    }

    /**
     * Fetch a single product by ID.
     * You can also cache this individually if needed.
     */
    @Cacheable(value = "productById", key = "#id")
    public Product getProductById(Long id) {
        log.info("Fetching product by ID {} from database...", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductSvcException(ERR_INVENTORY_MS_NO_PRODUCT_FOUND));
    }

    /**
     * Create a new product — clear all cache since data changes.
     */
    @CacheEvict(value = {"allProducts", "productById"}, allEntries = true)
    public Product createProduct(ProductRequest request) throws ProductSvcException {
        productRepository.findByProductName(request.getProductName())
                .ifPresent(p -> {
                    throw new ProductSvcException(ProductSvcErrorType.ERR_INVENTORY_MS_PRODUCT_EXIST);
                });

        Product newProduct = Product.builder()
                .id(ProductIdGenerator.generateId())
                .productName(request.getProductName())
                .description(request.getDescription())
                .productType(request.getProductType())
                .unitPrice(request.getUnitPrice())
                .quantity(request.getQuantity())
                .build();

        log.info("Created new product: {}", newProduct);
        return productRepository.save(newProduct);
    }

    /**
     * Update an existing product — also clear caches.
     */
    @CacheEvict(value = {"allProducts", "productById"}, allEntries = true)
    public Product updateProduct(Long id, ProductRequest request) throws ProductSvcException {
        log.info("Fetching product to update...");
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductSvcException(ERR_INVENTORY_MS_NO_PRODUCT_FOUND));

        log.info("Product found: {}", product);

        String newName = request.getProductName();
        String currentName = product.getProductName();

        // Check if name already exists (when changed)
        if (!currentName.equalsIgnoreCase(newName)) {
            productRepository.findByProductName(newName)
                    .ifPresent(p -> {
                        throw new ProductSvcException(ProductSvcErrorType.ERR_INVENTORY_MS_PRODUCT_EXIST);
                    });
        }

        // Apply updates
        updateProductFields(product, request);
        Product updated = productRepository.save(product);

        log.info("Updated product: {}", updated);
        return updated;
    }

    private void updateProductFields(Product product, ProductRequest request) {
        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setProductType(request.getProductType());
        product.setQuantity(request.getQuantity());
        product.setUnitPrice(request.getUnitPrice());
    }

    /**
     * Delete a product — clear all cached products.
     */
    @CacheEvict(value = {"allProducts", "productById"}, allEntries = true)
    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            log.info("Deleted product with ID {}", id);
            return true;
        }
        log.warn("Product with ID {} not found for deletion", id);
        return false;
    }
}
