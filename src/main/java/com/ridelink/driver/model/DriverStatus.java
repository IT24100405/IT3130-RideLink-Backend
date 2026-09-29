package com.ridelink.driver.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents the availability and trip status of a driver")
public enum DriverStatus {
    AVAILABLE,
    ON_TRIP,
    OFFLINE
}
