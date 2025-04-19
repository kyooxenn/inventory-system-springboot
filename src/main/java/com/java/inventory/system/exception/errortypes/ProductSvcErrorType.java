package com.java.inventory.system.exception.errortypes;

import com.java.inventory.system.exception.ErrorType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import static com.java.inventory.system.constant.ErrorConstants.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Getter
@AllArgsConstructor
public enum ProductSvcErrorType implements ErrorType {


    ERR_INVENTORY_MS_NO_PRODUCT_FOUND(INVENTORY_MS_ERR_CODE_001,
            INVENTORY_MS_NO_PRODUCT_FOUND, BAD_REQUEST),
    ERR_INVENTORY_MS_PRODUCT_EXIST(INVENTORY_MS_ERR_CODE_002,
            INVENTORY_MS_PRODUCT_EXIST, BAD_REQUEST);

    private String code;
    @Setter
    private String desc;
    @Setter
    private HttpStatus httpStatusCode;
}
