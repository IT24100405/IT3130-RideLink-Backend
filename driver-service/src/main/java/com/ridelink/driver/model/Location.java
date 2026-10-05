package com.ridelink.driver.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents geographic coordinates and service area of a driver")
public class Location {

    @Schema(description = "Latitude coordinate of the driver", example = "6.9271")
    private Double latitude;

    @Schema(description = "Longitude coordinate of the driver", example = "79.8612")
    private Double longitude;

    @Schema(description = "Service area name (e.g. Colombo, Kandy)", example = "Colombo")
    private String serviceArea;
}
