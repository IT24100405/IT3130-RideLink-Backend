package com.ridelink.ride.dto;

import com.ridelink.ride.model.Location;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed representation of ride details and status")
public class RideResponse {

    @Schema(description = "Unique MongoDB ObjectId of the ride", example = "64f1a2b3c4d5e6f7a8b9c0d1")
    private String id;

    @Schema(description = "Unique identifier of the passenger who created the ride request", example = "64f1a2b3c4d5e6f7a8b9c0a1")
    private String passengerId;

    @Schema(description = "Unique identifier of the assigned driver", example = "64f1a2b3c4d5e6f7a8b9c0b2", nullable = true)
    private String driverId;

    @Schema(description = "Pickup location coordinates and address")
    private Location pickupLocation;

    @Schema(description = "Destination location coordinates and address")
    private Location destinationLocation;

    @Schema(description = "Route distance in kilometers", example = "8.5")
    private Double distanceKm;

    @Schema(description = "Estimated fare calculated upon ride creation", example = "18.25")
    private Double estimatedFare;

    @Schema(description = "Final fare charged upon ride completion", example = "19.50", nullable = true)
    private Double finalFare;

    @Schema(description = "Current lifecycle status of the ride", example = "REQUESTED")
    private RideStatus status;

    @Schema(description = "Cancellation reason if the ride was cancelled", example = "Driver was unavailable", nullable = true)
    private String cancellationReason;

    @Schema(description = "Timestamp when the ride was created")
    private Instant createdAt;

    @Schema(description = "Timestamp when the ride was last updated")
    private Instant updatedAt;

    public static RideResponse fromEntity(Ride ride) {
        if (ride == null) {
            return null;
        }
        return RideResponse.builder()
                .id(ride.getId())
                .passengerId(ride.getPassengerId())
                .driverId(ride.getDriverId())
                .pickupLocation(ride.getPickupLocation())
                .destinationLocation(ride.getDestinationLocation())
                .distanceKm(ride.getDistanceKm())
                .estimatedFare(ride.getEstimatedFare())
                .finalFare(ride.getFinalFare())
                .status(ride.getStatus())
                .cancellationReason(ride.getCancellationReason())
                .createdAt(ride.getCreatedAt())
                .updatedAt(ride.getUpdatedAt())
                .build();
    }
}
