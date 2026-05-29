package com.java.inventory.system.exception.errortypes;

import com.java.inventory.system.exception.ErrorType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import static com.java.inventory.system.constant.ErrorConstants.*;
import static org.springframework.http.HttpStatus.*;

@Getter
@AllArgsConstructor
public enum NVentSvcErrorType implements ErrorType {


    ERR_INVENTORY_MS_NO_PRODUCT_FOUND(INVENTORY_MS_ERR_CODE_001,
            INVENTORY_MS_NO_PRODUCT_FOUND, BAD_REQUEST),
    ERR_INVENTORY_MS_PRODUCT_EXIST(INVENTORY_MS_ERR_CODE_002,
            INVENTORY_MS_PRODUCT_EXIST, BAD_REQUEST),
    ERR_CLIENT_MAXIMUM_ATTEMPT("429",
            "Maximum resend attempts reached. Please try again in %d minutes and %d seconds.", TOO_MANY_REQUESTS),
    ERR_CLIENT_INVALID_CREDENTIALS("401",
            "Invalid username or password.", UNAUTHORIZED),
    ERR_CLIENT_USER_NOT_FOUND("400",
            "User not found.", BAD_REQUEST),
    INTERNAL_SERVER_ERR("500",
            "An unexpected error occurred", INTERNAL_SERVER_ERROR),
    ERR_CLIENT_CREDENTIALS_EXISTS("409",
            "Email or mobile number already exists. Please use different credentials.", CONFLICT);

    private String code;
    @Setter
    private String desc;
    @Setter
    private HttpStatus httpStatusCode;
}
