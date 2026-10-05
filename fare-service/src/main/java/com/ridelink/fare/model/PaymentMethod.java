package com.ridelink.fare.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simulated payment methods supported for ride settlement")
public enum PaymentMethod {
    SIMULATED_CASH,
    SIMULATED_CARD,
    SIMULATED_WALLET
}
