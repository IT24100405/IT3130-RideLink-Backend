package com.ridelink.ride.dto;

import com.ridelink.ride.model.Location;
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
@Schema(description = "Request payload sent to fare-service for fare estimation")
public class FareEstimateRequest {

    @Schema(description = "Pickup location details")
    private Location pickupLocation;

    @Schema(description = "Destination location details")
    private Location destinationLocation;

    @NotNull(message = "Distance in km is required")
    @Positive(message = "Distance must be greater than zero")
    @Schema(description = "Route distance in kilometers", example = "12.5")
    private Double distanceKm;
}
