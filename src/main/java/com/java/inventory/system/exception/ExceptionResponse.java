package com.java.inventory.system.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExceptionResponse {
    private String errorCode;
    private String errorMessage;

    private String fieldName;      // Optional: Field that caused the error
    private String timestamp;      // Helpful for logging: When the error occurred
    private String path;           // API endpoint or method where the error happened
    private String exceptionType;  // Class name of the thrown exception
    private List<String> details;  // For additional context or validation errors

}