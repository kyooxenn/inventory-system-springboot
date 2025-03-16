package com.java.inventory.system.apidocs;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success")}
)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiDocSuccessResponse {
}