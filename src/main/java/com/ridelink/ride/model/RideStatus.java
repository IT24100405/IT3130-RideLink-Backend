package com.ridelink.ride.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of a ride request", enumAsRef = true)
public enum RideStatus {
    REQUESTED,
    ASSIGNED,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
