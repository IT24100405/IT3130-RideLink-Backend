package com.ridelink.fare.dto;

import com.ridelink.fare.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request payload to process and record a ride fare payment")
public class ProcessPaymentRequest {

    @NotBlank(message = "Ride ID is mandatory")
    @Schema(description = "Unique identifier of the completed ride", example = "64f1a2b3c4d5e6f7a8b9c0a1")
    private String rideId;

    @Schema(hidden = true)
    private String passengerId;

    @Schema(hidden = true)
    private String driverId;

    @NotNull(message = "Distance in kilometers is mandatory")
    @Positive(message = "Distance must be strictly positive")
    @Schema(description = "Total distance in kilometers traveled during the completed ride", example = "12.5")
    private Double distanceKm;

    @NotNull(message = "Payment method is mandatory")
    @Schema(description = "Selected payment method for settlement", example = "SIMULATED_CARD")
    private PaymentMethod paymentMethod;

    @Builder.Default
    @Schema(description = "Optional flag to simulate payment rejection for negative scenario testing", example = "false")
    private Boolean simulateFailure = false;
}
