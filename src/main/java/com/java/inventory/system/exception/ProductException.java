package com.java.inventory.system.exception;

import lombok.Getter;
import org.apache.logging.log4j.util.Strings;

@Getter
public class ProductException extends RuntimeException {
    private final String code;
    private final String description;

    public ProductException(String code, String message) {
        this(code, message, Strings.EMPTY);
    }

    public ProductException(String code, String message, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }
}
