package com.java.inventory.system.service;

import com.java.inventory.system.dto.ProductRequest;
import com.java.inventory.system.exception.ProductSvcException;
import com.java.inventory.system.exception.errortypes.ProductSvcErrorType;
import com.java.inventory.system.model.Product;
import com.java.inventory.system.repository.ProductRepository;
import com.java.inventory.system.util.ProductIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.java.inventory.system.exception.errortypes.ProductSvcErrorType.ERR_INVENTORY_MS_NO_PRODUCT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }


    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

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

        return productRepository.save(newProduct);
    }

    public Product updateProduct(Long id, ProductRequest request) throws ProductSvcException {
        log.info("fetch product...");
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductSvcException(ERR_INVENTORY_MS_NO_PRODUCT_FOUND));

        log.info("product found! {}", product);

        // Update the fields of the existing product
        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setProductType(request.getProductType());
        product.setQuantity(request.getQuantity());
        product.setUnitPrice(request.getUnitPrice());

        log.info("updated product {}", product);

        // Save the updated product back to the repository
        return productRepository.save(product);  // This will update the existing entity
    }


    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
