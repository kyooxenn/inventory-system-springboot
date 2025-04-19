package com.java.inventory.system.controller;

import com.java.inventory.system.exception.ExceptionResponse;
import com.java.inventory.system.exception.ProductSvcException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(assignableTypes = {ProductController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ControllerAdvice {

    @ExceptionHandler(ProductSvcException.class)
    public final ResponseEntity<ExceptionResponse> handleProductSvcException(ProductSvcException ex, WebRequest request) {
        log.error("Exception occurred: {} - {}", ex.getError().getCode(), ex.getMessage());

        ExceptionResponse response = ExceptionResponse.builder()
                .errorCode(ex.getError().getCode())
                .errorMessage(ex.getError().getDesc())
                .exceptionType(ex.getClass().getSimpleName())
                .timestamp(Instant.now().toString())  // Automatically add time
                .path(request.getDescription(false)) // API endpoint or request description
                .build();

        return new ResponseEntity<>(response, ex.getError().getHttpStatusCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public final ResponseEntity<ExceptionResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        log.error("Validation error occurred: {}", validationErrors);

        ExceptionResponse response = ExceptionResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .errorMessage("Validation failed for one or more fields.")
                .exceptionType(ex.getClass().getSimpleName())
                .timestamp(Instant.now().toString())
                .path(request.getDescription(false))
                .details(validationErrors) // Capture multiple validation errors
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
