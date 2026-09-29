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
@Schema(description = "Represents vehicle details associated with a driver")
public class Vehicle {

    @Schema(description = "License plate / registration number of the vehicle", example = "CAB-1234")
    private String vehicleNumber;

    @Schema(description = "Make and model of the vehicle", example = "Toyota Prius")
    private String model;

    @Schema(description = "Type of vehicle", example = "CAR")
    private VehicleType vehicleType;

    @Schema(description = "Color of the vehicle", example = "White")
    private String color;
}
