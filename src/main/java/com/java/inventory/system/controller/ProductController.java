package com.java.inventory.system.controller;

import com.java.inventory.system.dto.ProductRequest;
import com.java.inventory.system.model.Product;
import com.java.inventory.system.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


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

    @GetMapping(value = "/{id}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        Optional<Product> product = productService.getProductById(id);
        return product.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping(value = "/{id}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();
        if (productService.deleteProduct(id)) {
            response.put("message", "Product deleted successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Product with id " + id + " not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}

