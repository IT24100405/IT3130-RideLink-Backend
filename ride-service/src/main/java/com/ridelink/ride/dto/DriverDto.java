package com.ridelink.ride.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO representing driver information received from driver-service")
public class DriverDto {

    @Schema(description = "Unique identifier of the driver", example = "64f1a2b3c4d5e6f7a8b9c0b2")
    private String driverId;

    @Schema(description = "Full name of the driver", example = "John Driver")
    private String fullName;

    @Schema(description = "Vehicle registration number", example = "CAB-1234")
    private String vehicleNumber;

    @JsonProperty("isAvailable")
    @JsonAlias({"available", "isAvailable"})
    @Schema(description = "Indicates whether the driver is currently available for dispatch", example = "true")
    private Boolean isAvailable;
}
