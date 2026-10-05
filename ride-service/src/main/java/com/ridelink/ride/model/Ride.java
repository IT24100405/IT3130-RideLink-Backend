package com.ridelink.ride.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "rides")
@Schema(description = "Represents a ride request lifecycle entity in MongoDB")
public class Ride {

    @Id
    @Schema(description = "Unique MongoDB ObjectId of the ride", example = "64f1a2b3c4d5e6f7a8b9c0d1")
    private String id;

    @NotBlank(message = "Passenger ID is mandatory")
    @Indexed
    @Schema(description = "Unique identifier of the passenger who created the ride request", example = "64f1a2b3c4d5e6f7a8b9c0a1")
    private String passengerId;

    @Indexed
    @Schema(description = "Unique identifier of the driver assigned to this ride (null until assigned)", example = "64f1a2b3c4d5e6f7a8b9c0b2", nullable = true)
    private String driverId;

    @Schema(description = "Pickup location coordinates and address")
    private Location pickupLocation;

    @Schema(description = "Destination location coordinates and address")
    private Location destinationLocation;

    @Schema(description = "Calculated route distance in kilometers", example = "12.5")
    private Double distanceKm;

    @Schema(description = "Estimated fare calculated by fare-service upon ride creation", example = "24.50")
    private Double estimatedFare;

    @Schema(description = "Final calculated fare charged upon ride completion", example = "25.00", nullable = true)
    private Double finalFare;

    @Builder.Default
    @Indexed
    @Schema(description = "Current lifecycle status of the ride", example = "REQUESTED")
    private RideStatus status = RideStatus.REQUESTED;

    @Schema(description = "Optional reason provided if the ride is cancelled", example = "Passenger requested cancellation", nullable = true)
    private String cancellationReason;

    @CreatedDate
    @Schema(description = "Timestamp when the ride request was created")
    private Instant createdAt;

    @LastModifiedDate
    @Schema(description = "Timestamp when the ride request was last updated")
    private Instant updatedAt;
}
