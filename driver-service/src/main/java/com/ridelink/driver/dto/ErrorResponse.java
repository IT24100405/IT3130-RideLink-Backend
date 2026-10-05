package com.ridelink.driver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error response structure")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error description or reason", example = "Bad Request")
    private String error;

    @Schema(description = "Detailed error message", example = "Validation failed for one or more fields")
    private String message;

    @Schema(description = "Request URI path where the error occurred", example = "/api/drivers")
    private String path;

    @Schema(description = "Field-level validation error details, if applicable")
    private Map<String, String> errors;

    @Schema(description = "Additional error details or field mappings, if applicable")
    private Map<String, String> details;


    @Builder.Default
    @Schema(description = "Timestamp when the error occurred")
    private Instant timestamp = Instant.now();
}
