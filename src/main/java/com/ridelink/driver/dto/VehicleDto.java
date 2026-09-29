package com.ridelink.driver.dto;

import com.ridelink.driver.model.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Vehicle details for driver registration")
public class VehicleDto {

    @NotBlank(message = "Vehicle number is mandatory")
    @Schema(description = "License plate / registration number", example = "CAB-1234")
    private String vehicleNumber;

    @NotBlank(message = "Vehicle model is mandatory")
    @Schema(description = "Make and model of vehicle", example = "Toyota Prius")
    private String model;

    @NotNull(message = "Vehicle type is mandatory")
    @Schema(description = "Vehicle type category", example = "CAR")
    private VehicleType vehicleType;

    @Schema(description = "Vehicle exterior color", example = "White")
    private String color;
}
