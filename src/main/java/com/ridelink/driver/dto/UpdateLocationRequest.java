package com.ridelink.driver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to update driver's current geographic location")
public class UpdateLocationRequest {

    @NotNull(message = "Latitude is mandatory")
    @Schema(description = "Current latitude coordinate", example = "6.9271")
    private Double latitude;

    @NotNull(message = "Longitude is mandatory")
    @Schema(description = "Current longitude coordinate", example = "79.8612")
    private Double longitude;

    @NotBlank(message = "Service area is mandatory")
    @Schema(description = "Current operating service area", example = "Colombo")
    private String serviceArea;
}
