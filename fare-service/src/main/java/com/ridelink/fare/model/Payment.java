package com.ridelink.fare.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
@Schema(description = "Represents a ride payment transaction entity in MongoDB")
public class Payment {

    @Id
    @Schema(description = "Unique MongoDB ObjectId of the payment", example = "64f1a2b3c4d5e6f7a8b9c0d1")
    private String id;

    @Indexed(unique = true)
    @Schema(description = "Unique foreign reference from ride-service ride ID", example = "64f1a2b3c4d5e6f7a8b9c0a1")
    private String rideId;

    @Indexed
    @Schema(description = "Unique identifier of the passenger", example = "64f1a2b3c4d5e6f7a8b9c0p1")
    private String passengerId;

    @Indexed
    @Schema(description = "Unique identifier of the driver", example = "64f1a2b3c4d5e6f7a8b9c0d2")
    private String driverId;

    @Schema(description = "Total distance in kilometers traveled for the ride", example = "12.5")
    private Double distanceKm;

    @Schema(description = "Standard base fare component applied to the ride", example = "150.0")
    private Double baseFare;

    @Schema(description = "Variable distance-based fare component", example = "1000.0")
    private Double distanceFare;

    @Schema(description = "Final total amount charged or estimated", example = "1150.0")
    private Double totalAmount;

    @Schema(description = "Payment method selected for transaction", example = "SIMULATED_CARD")
    private PaymentMethod paymentMethod;

    @Indexed
    @Schema(description = "Current status of the payment transaction", example = "COMPLETED")
    private PaymentStatus status;

    @Indexed(unique = true)
    @Schema(description = "Unique alphanumeric receipt identifier generated for the transaction", example = "REC-2026-987654")
    private String receiptNumber;

    @Schema(description = "Reason for failure if payment simulation fails", example = "Card declined due to insufficient simulated balance", nullable = true)
    private String failureReason;

    @CreatedDate
    @Schema(description = "Timestamp when the payment was executed or recorded")
    private Instant timestamp;
}
