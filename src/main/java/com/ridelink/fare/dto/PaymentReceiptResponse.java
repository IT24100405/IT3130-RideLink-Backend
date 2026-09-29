package com.ridelink.fare.dto;

import com.ridelink.fare.model.Payment;
import com.ridelink.fare.model.PaymentMethod;
import com.ridelink.fare.model.PaymentStatus;
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
@Schema(description = "Detailed receipt containing breakdown of fares, total, status, and transaction timestamp")
public class PaymentReceiptResponse {

    @Schema(description = "Unique alphanumeric receipt identifier", example = "REC-2026-987654")
    private String receiptNumber;

    @Schema(description = "Unique ride identifier associated with this payment", example = "64f1a2b3c4d5e6f7a8b9c0a1")
    private String rideId;

    @Schema(description = "Passenger identifier", example = "64f1a2b3c4d5e6f7a8b9c0p1")
    private String passengerId;

    @Schema(description = "Driver identifier", example = "64f1a2b3c4d5e6f7a8b9c0d2")
    private String driverId;

    @Schema(description = "Distance in kilometers", example = "12.5")
    private Double distanceKm;

    @Schema(description = "Base fare component applied", example = "150.0")
    private Double baseFare;

    @Schema(description = "Distance-based fare component", example = "1000.0")
    private Double distanceFare;

    @Schema(description = "Final calculated total fare amount", example = "1150.0")
    private Double totalAmount;

    @Builder.Default
    @Schema(description = "Currency unit", example = "LKR")
    private String currency = "LKR";

    @Schema(description = "Payment method used", example = "SIMULATED_CARD")
    private PaymentMethod paymentMethod;

    @Schema(description = "Transaction settlement status", example = "COMPLETED")
    private PaymentStatus status;

    @Schema(description = "Reason for failure if payment simulation fails", example = "Simulated payment failure triggered", nullable = true)
    private String failureReason;

    @Schema(description = "Timestamp when the payment transaction occurred")
    private Instant timestamp;

    public static PaymentReceiptResponse fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        return PaymentReceiptResponse.builder()
                .receiptNumber(payment.getReceiptNumber())
                .rideId(payment.getRideId())
                .passengerId(payment.getPassengerId())
                .driverId(payment.getDriverId())
                .distanceKm(payment.getDistanceKm())
                .baseFare(payment.getBaseFare())
                .distanceFare(payment.getDistanceFare())
                .totalAmount(payment.getTotalAmount())
                .currency("LKR")
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .failureReason(payment.getFailureReason())
                .timestamp(payment.getTimestamp())
                .build();
    }
}
