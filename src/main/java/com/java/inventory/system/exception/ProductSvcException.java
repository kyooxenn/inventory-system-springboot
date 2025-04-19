package com.java.inventory.system.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ProductSvcException extends BaseException {

    public ProductSvcException(ErrorType error) {
        super(error);
    }

    public ProductSvcException(ErrorType error, Throwable cause) {
        super(error, cause);
    }

    public ProductSvcException(String fieldName, ErrorType error) {
        super(fieldName, error);
    }
}
