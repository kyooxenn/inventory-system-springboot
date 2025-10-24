package com.java.inventory.system.controller;

import com.java.inventory.system.apidocs.ApiDocAllErrorsResponse;
import com.java.inventory.system.apidocs.ApiDocSuccessResponse;
import com.java.inventory.system.dto.ProductRequest;
import com.java.inventory.system.exception.ProductSvcException;
import com.java.inventory.system.model.Product;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

public interface ProductApi {

    @Operation(description = "Get All Products", tags = {"All Products Inquiry"})
    @ApiDocSuccessResponse
    @ApiDocAllErrorsResponse
    ResponseEntity<Map<String, Object>> getAllProducts(@PageableDefault(size = 10, sort = "itemName") Pageable pageable);

    @Operation(description = "Get Product", tags = {"Products Inquiry"})
    @ApiDocSuccessResponse
    @ApiDocAllErrorsResponse
    ResponseEntity<Map<String, Object>> getProductsByNameAndCategory(@RequestParam(required = false) String itemName,
                                                               @RequestParam(required = false) String category,
                                                               @PageableDefault(size = 5, sort = "itemName") Pageable pageable);

    @Operation(description = "Get Product", tags = {"Products Inquiry"})
    @ApiDocSuccessResponse
    @ApiDocAllErrorsResponse
    ResponseEntity<Product> getProductById(@PathVariable String id);

    @Operation(description = "Create new product", tags = {"Add Product"})
    @ApiDocSuccessResponse
    @ApiDocAllErrorsResponse
    ResponseEntity<Product> createProduct(@Valid @RequestBody ProductRequest request) throws ProductSvcException;

    @Operation(description = "Update product", tags = {"Update product"})
    @ApiDocSuccessResponse
    @ApiDocAllErrorsResponse
    ResponseEntity<Product> updateProduct(@PathVariable String id, @Valid @RequestBody ProductRequest request) throws ProductSvcException;

    @Operation(description = "Delete product", tags = {"delete product"})
    @ApiDocSuccessResponse
    @ApiDocAllErrorsResponse
    ResponseEntity<Map<String, String>> deleteProduct(@PathVariable String id);
}