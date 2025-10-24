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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static com.java.inventory.system.exception.errortypes.ProductSvcErrorType.ERR_INVENTORY_MS_NO_PRODUCT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Cache the result of fetching all products per page.
     * Each unique page (page number + size + sort) is cached separately.
     * Note: Cache eviction should be configured for updates/deletes (e.g., @CacheEvict on save/delete methods).
     */
    @Cacheable(
            value = "allProducts",
            key = "'page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize + '_sort_' + #pageable.sort.toString()"
    )
    public Map<String, Object> getAllProducts(Pageable pageable) {
        log.info("Fetching products from database (page={}, size={})...", pageable.getPageNumber(), pageable.getPageSize());
        Page<Product> page = productRepository.findAll(pageable);

        Map<String, Object> response = buildResponse(page);

        log.info("Products retrieved successfully: {} items on this page, {} total.",
                page.getNumberOfElements(), page.getTotalElements());

        return response;
    }

    /**
     * Fetch products by name and category with pagination.
     * Caching added for performance if searches are frequent.
     * Note: Cache eviction should be configured for updates/deletes.
     */
    @Cacheable(
            value = "searchProducts",
            key = "'search_' + #productName + '_' + #category + '_page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize + '_sort_' + #pageable.sort.toString()"
    )
    public Map<String, Object> findByItemNameAndCategory(String productName, String category, Pageable pageable) {
        log.info("Searching products from database (name={}, category={}, page={}, size={})...",
                productName, category, pageable.getPageNumber(), pageable.getPageSize());

        Page<Product> page = productRepository.findByItemNameAndCategory(productName, category, pageable);

        Map<String, Object> response = buildResponse(page);

        log.info("Search completed: {} items on this page, {} total.",
                page.getNumberOfElements(), page.getTotalElements());

        return response;
    }

    /**
     * Helper method to build the response map, reducing code duplication.
     */
    private Map<String, Object> buildResponse(Page<Product> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", page.getContent());
        response.put("currentPage", page.getNumber());
        response.put("totalItems", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        return response;
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
