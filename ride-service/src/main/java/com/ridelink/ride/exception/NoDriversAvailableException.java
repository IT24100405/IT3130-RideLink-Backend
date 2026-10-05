package com.ridelink.ride.exception;

public class NoDriversAvailableException extends RuntimeException {

    public NoDriversAvailableException(String message) {
        super(message);
    }

    public NoDriversAvailableException() {
        super("No eligible drivers available in this area");
    }
}
