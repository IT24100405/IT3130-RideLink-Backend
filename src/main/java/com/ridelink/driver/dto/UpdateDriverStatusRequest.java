package com.ridelink.driver.dto;

import com.ridelink.driver.model.DriverStatus;
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
@Schema(description = "Request payload to update driver operational availability status")
public class UpdateDriverStatusRequest {

    @NotNull(message = "Driver status is mandatory")
    @Schema(description = "New operational status for the driver", example = "AVAILABLE")
    private DriverStatus status;
}
