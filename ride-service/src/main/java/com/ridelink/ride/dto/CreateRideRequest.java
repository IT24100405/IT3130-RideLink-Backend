package com.ridelink.ride.dto;

import com.ridelink.ride.model.Location;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Payload required to request a new ride")
public class CreateRideRequest {

    @Schema(hidden = true)
    private String passengerId;

    @NotNull(message = "Pickup location is required")
    @Valid
    @Schema(description = "Starting location of the ride", requiredMode = Schema.RequiredMode.REQUIRED)
    private Location pickupLocation;

    @NotNull(message = "Destination location is required")
    @Valid
    @Schema(description = "Drop-off destination location of the ride", requiredMode = Schema.RequiredMode.REQUIRED)
    private Location destinationLocation;

    @NotNull(message = "Distance in km is required")
    @Positive(message = "Distance must be greater than zero")
    @Schema(description = "Calculated route distance in kilometers", example = "8.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double distanceKm;
}
