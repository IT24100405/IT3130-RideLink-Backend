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
    @Schema(description = "Route distance in kilometers", example = "8.5")
    private Double distanceKm;

    @Schema(description = "Pickup latitude coordinate", example = "6.927")
    private Double pickupLatitude;

    @Schema(description = "Pickup longitude coordinate", example = "79.861")
    private Double pickupLongitude;

    @Schema(description = "Destination latitude coordinate", example = "6.9")
    private Double destinationLatitude;

    @Schema(description = "Destination longitude coordinate", example = "79.95")
    private Double destinationLongitude;
}
