package com.ridelink.fare.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of a payment transaction")
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED
}
