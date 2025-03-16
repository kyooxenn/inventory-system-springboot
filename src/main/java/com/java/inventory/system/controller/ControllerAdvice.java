package com.java.inventory.system.controller;

import com.java.inventory.system.exception.ProductException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestControllerAdvice(assignableTypes = {ProductController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ControllerAdvice {

    /**
     * Helper method to build the common error response structure.
     */
    private Map<String, Object> buildErrorResponse(HttpStatus status, String errorMessage, Map<String, Object> extraData) {
        Map<String, Object> response = new HashMap<>();
        response.put("uniqueId", "error-" + UUID.randomUUID().toString());
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status.name());
        response.put("httpCode", status.value());
        response.put("error", errorMessage);

        // In a real-world application, you might integrate a tracing system to get an actual trace ID.
        response.put("traceId", UUID.randomUUID().toString());

        if (extraData != null) {
            response.putAll(extraData);
        }
        return response;
    }

    // Handler for validation errors (400 Bad Request)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("errors", fieldErrors);

        Map<String, Object> response = buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", extraData);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handler for ProductException (404 Not Found)
    @ExceptionHandler(ProductException.class)
    public ResponseEntity<Map<String, Object>> handleProductException(ProductException ex) {
        Map<String, Object> response = buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // Generic handler for all other exceptions (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception caught", ex);
        Map<String, Object> response = buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, Object> errorResponse = new HashMap<>();

        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");

        // Get the detailed technical message from the exception
        String technicalMessage = ex.getMostSpecificCause().getMessage();

        // Use regex to extract all field names referenced in the error message (e.g., ["quantity"], ["unitPrice"])
        Pattern pattern = Pattern.compile("\\[\"(.*?)\"\\]");
        Matcher matcher = pattern.matcher(technicalMessage);
        List<String> fields = new ArrayList<>();
        while (matcher.find()) {
            fields.add(matcher.group(1));
        }

        String userFriendlyMessage;
        if (!fields.isEmpty()) {
            userFriendlyMessage = "Invalid input for field(s): " + String.join(", ", fields) +
                    ". Please ensure they have valid numeric values.";
        } else {
            userFriendlyMessage = "The JSON request is invalid. Please check that all numeric fields have valid numeric values.";
        }
        errorResponse.put("message", userFriendlyMessage);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

}
