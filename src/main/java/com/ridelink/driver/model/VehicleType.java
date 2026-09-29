package com.ridelink.driver.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category/type of the vehicle driven by the driver")
public enum VehicleType {
    CAR,
    VAN,
    BIKE,
    TUK
}
