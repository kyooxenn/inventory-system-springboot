package com.java.inventory.system.controller;

import com.java.inventory.system.exception.BaseException;
import com.java.inventory.system.exception.ErrorType;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ControllerAdvice {

    @ExceptionHandler(ProductSvcException.class)
    public final ResponseEntity<ExceptionResponse> handleProductSvcException(ProductSvcException ex, WebRequest request) {
        log.error("Exception occurred: {} - {}", ex.getError().getCode(), ex.getMessage());

        ExceptionResponse response = ExceptionResponse.builder()
                .errorCode(ex.getError().getCode())
                .errorMessage(ex.getError().getDesc())
                .exceptionType(ex.getClass().getSimpleName())
                .timestamp(LocalDateTime.now())  // Automatically add time
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
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false))
                .details(validationErrors) // Capture multiple validation errors
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ✅ NEW: Handle BaseException
    @ExceptionHandler(BaseException.class)
    public final ResponseEntity<ExceptionResponse> handleBaseException(BaseException ex, WebRequest request) {
        // Log the exception with appropriate level
        ErrorType error = ex.getError();
        String errorCode = error != null ? error.getCode() : "BASE_EXCEPTION";
        String errorMessage = ex.getMessage();

        log.error("BaseException occurred - Code: {}, Message: {}, Field: {}",
                errorCode, errorMessage, ex.getFieldName());

        // Build exception response
        ExceptionResponse.ExceptionResponseBuilder responseBuilder = ExceptionResponse.builder()
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .exceptionType(ex.getError().toString())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false));

        // Add field name if present
        if (ex.getFieldName() != null && !ex.getFieldName().isEmpty()) {
            responseBuilder.details(List.of("Field: " + ex.getFieldName()));
        }

       /* // Add error description from ErrorType if available and different from message
        if (error != null && !error.getDesc().equals(errorMessage)) {
            responseBuilder.details(List.of("Error Type: " + error.getDesc()));
        }*/

        ExceptionResponse response = responseBuilder.build();

        // Determine HTTP status based on error type
        HttpStatus status = determineHttpStatus(error);

        return new ResponseEntity<>(response, status);
    }

    // Helper method to determine HTTP status from ErrorType
    private HttpStatus determineHttpStatus(ErrorType error) {
        if (error == null) {
            return HttpStatus.BAD_REQUEST;
        }

        // Check if ErrorType has getHttpStatusCode method
        try {
            return error.getHttpStatusCode();
        } catch (Exception e) {
            // If ErrorType doesn't have HTTP status, determine based on error code pattern
            String errorCode = error.getCode();
            if (errorCode != null) {
                if (errorCode.contains("404")) {
                    return HttpStatus.NOT_FOUND;
                } else if (errorCode.contains("401")) {
                    return HttpStatus.UNAUTHORIZED;
                } else if (errorCode.contains("403")) {
                    return HttpStatus.FORBIDDEN;
                } else if (errorCode.contains("429")) {
                    return HttpStatus.TOO_MANY_REQUESTS;
                } else if (errorCode.contains("400")) {
                    return HttpStatus.BAD_REQUEST;
                } else if (errorCode.contains("409")) {
                    return HttpStatus.CONFLICT;
                }
            }
            return HttpStatus.BAD_REQUEST;
        }
    }
}
