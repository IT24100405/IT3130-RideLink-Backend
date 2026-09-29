package com.ridelink.ride.exception;

public class RideNotFoundException extends ResourceNotFoundException {

    public RideNotFoundException(String message) {
        super(message);
    }

    public RideNotFoundException(String rideId, boolean isId) {
        super("Ride not found with ID: " + rideId);
    }
}
