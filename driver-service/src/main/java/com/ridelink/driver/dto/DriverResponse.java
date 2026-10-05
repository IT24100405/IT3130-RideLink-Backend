package com.ridelink.driver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ridelink.driver.model.Driver;
import com.ridelink.driver.model.DriverStatus;
import com.ridelink.driver.model.Location;
import com.ridelink.driver.model.Vehicle;
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
@Schema(description = "Driver operational profile, vehicle specifications, availability status, and current location")
public class DriverResponse {

    @Schema(description = "Unique MongoDB ObjectId of the driver", example = "64f1a2b3c4d5e6f7a8b9c0d1")
    private String id;

    @Schema(description = "Driver ID alias for inter-service communication", example = "64f1a2b3c4d5e6f7a8b9c0d1")
    private String driverId;

    @Schema(description = "Foreign user ID reference from account-service", example = "64f1a2b3c4d5e6f7a8b9c0a1")
    private String userId;

    @Schema(description = "Full name of the driver", example = "John Silva")
    private String fullName;

    @Schema(description = "Contact phone number of the driver", example = "+94771234567")
    private String phoneNumber;

    @Schema(description = "Official driving license number", example = "B1234567")
    private String licenseNumber;

    @Schema(description = "Vehicle specifications")
    private Vehicle vehicle;

    @Schema(description = "Vehicle license plate number", example = "CAB-1234")
    private String vehicleNumber;

    @Schema(description = "Current operational availability status", example = "AVAILABLE")
    private DriverStatus status;

    @JsonProperty("isAvailable")
    @Schema(description = "Boolean indicator whether driver is currently available for dispatch", example = "true")
    private Boolean isAvailable;

    @Schema(description = "Current geographic location and service area")
    private Location currentLocation;

    @Schema(description = "Timestamp when the driver profile was created")
    private Instant createdAt;

    @Schema(description = "Timestamp when the driver profile was last updated")
    private Instant updatedAt;

    public static DriverResponse fromEntity(Driver driver) {
        if (driver == null) {
            return null;
        }
        return DriverResponse.builder()
                .id(driver.getId())
                .driverId(driver.getId())
                .userId(driver.getUserId())
                .fullName(driver.getFullName())
                .phoneNumber(driver.getPhoneNumber())
                .licenseNumber(driver.getLicenseNumber())
                .vehicle(driver.getVehicle())
                .vehicleNumber(driver.getVehicle() != null ? driver.getVehicle().getVehicleNumber() : null)
                .status(driver.getStatus())
                .isAvailable(driver.getStatus() == DriverStatus.AVAILABLE)
                .currentLocation(driver.getCurrentLocation())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }
}
