package com.ridelink.driver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
@Schema(description = "Request body to register/create a new driver profile")
public class CreateDriverRequest {

    @Schema(hidden = true)
    private String userId;

    @NotBlank(message = "Full name is mandatory")
    @Schema(description = "Full legal name of the driver", example = "John Silva")
    private String fullName;

    @NotBlank(message = "Phone number is mandatory")
    @Schema(description = "Contact phone number of the driver", example = "+94771234567")
    private String phoneNumber;

    @NotBlank(message = "License number is mandatory")
    @Schema(description = "Official driver license number", example = "B1234567")
    private String licenseNumber;

    @Valid
    @NotNull(message = "Vehicle details are mandatory")
    @Schema(description = "Vehicle specifications including registration number, model, and vehicle type")
    private VehicleDto vehicle;
}
