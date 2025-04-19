package com.java.inventory.system.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

@Data
@EqualsAndHashCode(callSuper = false)
public class BaseException extends RuntimeException {

    private ErrorType error;
    private String fieldName;

    public BaseException() {
        super();
    }

    public BaseException(ErrorType error) {
        super(error == null ? StringUtils.EMPTY : error.getDesc());
        this.error = error;
    }

    public BaseException(ErrorType error, String message) {
        super(message);
        this.error = error;
    }

    public BaseException(ErrorType error, Throwable cause) {
        super(error == null ? StringUtils.EMPTY : error.getDesc(), cause);
        this.error = error;
    }

    public BaseException(ErrorType error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public BaseException(String fieldName, ErrorType error) {
        super(error == null ? StringUtils.EMPTY : error.getDesc());
        this.fieldName = fieldName;
        this.error = error;
    }
}
