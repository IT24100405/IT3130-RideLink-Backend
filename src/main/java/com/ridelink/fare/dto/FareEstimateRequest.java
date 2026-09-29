package com.ridelink.fare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to calculate transparent fare estimation")
public class FareEstimateRequest {

    @NotNull(message = "Distance in kilometers is mandatory")
    @Positive(message = "Distance must be strictly positive")
    @Schema(description = "Route distance in kilometers", example = "12.5")
    private Double distanceKm;

    @Schema(description = "Optional pickup location details", nullable = true)
    private Object pickupLocation;

    @Schema(description = "Optional destination location details", nullable = true)
    private Object destinationLocation;
}
