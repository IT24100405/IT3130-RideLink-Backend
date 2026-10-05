package com.ridelink.ride.dto;

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
@Schema(description = "Standardized error response payload")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP error phrase", example = "Bad Request")
    private String error;

    @Schema(description = "Descriptive error message", example = "Invalid ride status transition")
    private String message;

    @Schema(description = "URI path of the requested resource", example = "/api/rides")
    private String path;

    @Schema(description = "Timestamp when the error occurred")
    private Instant timestamp;

    @Schema(description = "Field-level validation error details, if applicable", nullable = true)
    private Map<String, String> details;
}
