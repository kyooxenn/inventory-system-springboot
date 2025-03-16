package com.java.inventory.system.apidocs;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@ApiResponses(value = {
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "Forbidden Access"),
    @ApiResponse(responseCode = "404", description = "Resource Not Found"),
    @ApiResponse(responseCode = "500", description = "Internal server error occurred")
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiDocAllErrorsResponse {
}
