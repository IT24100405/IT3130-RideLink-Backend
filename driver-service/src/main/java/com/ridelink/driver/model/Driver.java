package com.ridelink.driver.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "drivers")
@Schema(description = "Represents a driver profile entity in MongoDB")
public class Driver {

    @Id
    @Schema(description = "Unique MongoDB ObjectId of the driver", example = "64f1a2b3c4d5e6f7a8b9c0d1")
    private String id;

    @Indexed(unique = true)
    @Schema(description = "Unique foreign reference from account-service user ID", example = "64f1a2b3c4d5e6f7a8b9c0a1")
    private String userId;

    @Schema(description = "Full legal name of the driver", example = "John Silva")
    private String fullName;

    @Indexed
    @Schema(description = "Driver contact phone number", example = "+94771234567")
    private String phoneNumber;

    @Indexed(unique = true)
    @Schema(description = "Driver official driving license number", example = "B1234567")
    private String licenseNumber;

    @Schema(description = "Registered vehicle information")
    private Vehicle vehicle;

    @Builder.Default
    @Indexed
    @Schema(description = "Current availability and trip status of the driver", example = "OFFLINE")
    private DriverStatus status = DriverStatus.OFFLINE;

    @Schema(description = "Last known geographic location and service area")
    private Location currentLocation;

    @CreatedDate
    @Schema(description = "Timestamp when the driver profile was created")
    private Instant createdAt;

}
