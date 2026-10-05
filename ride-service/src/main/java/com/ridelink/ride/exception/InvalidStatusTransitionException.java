package com.ridelink.ride.exception;

import com.ridelink.ride.model.RideStatus;
import lombok.Getter;

@Getter
public class InvalidStatusTransitionException extends RuntimeException {

    private final RideStatus currentStatus;
    private final RideStatus targetStatus;

    public InvalidStatusTransitionException(String message) {
        super(message);
        this.currentStatus = null;
        this.targetStatus = null;
    }

    public InvalidStatusTransitionException(RideStatus currentStatus, RideStatus targetStatus) {
        super(String.format("Invalid ride status transition: Cannot transition ride from %s to %s", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }
}
