package com.ridelink.ride.dto;

import com.ridelink.ride.model.RideStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload required to update the status of an existing ride")
public class UpdateRideStatusRequest {

    @NotNull(message = "Ride status is required")
    @Schema(description = "New lifecycle state of the ride", example = "ACCEPTED", requiredMode = Schema.RequiredMode.REQUIRED)
    private RideStatus status;

    @Schema(hidden = true)
    private String driverId;

    @Schema(description = "Reason for ride cancellation (applicable when status is CANCELLED)", example = "Passenger requested cancellation", nullable = true)
    private String reason;
}
