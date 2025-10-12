package com.java.inventory.system.controller;

import com.java.inventory.system.dto.ProductRequest;
import com.java.inventory.system.exception.ProductSvcException;
import com.java.inventory.system.model.Product;
import com.java.inventory.system.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@CrossOrigin
@RestController
@RequestMapping(value = "/v1/product", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class ProductController implements ProductApi {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping(value = "/details/{productName}")
    public ResponseEntity<List<Product>> getProduct(@PathVariable String productName) {
        return ResponseEntity.ok(productService.getProductByProductName(productName));
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductRequest request) throws ProductSvcException {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @Valid @RequestBody ProductRequest request)
            throws ProductSvcException {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable String id) {
        return productService.deleteProduct(id) ? ResponseEntity.ok(Map.of("message", "Product deleted successfully.")) :
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Product with id " + id + " not found."));
    }
}

