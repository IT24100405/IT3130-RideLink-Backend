package com.ridelink.ride.service;

import com.ridelink.ride.client.AccountServiceClient;
import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.DriverDto;
import com.ridelink.ride.dto.FareEstimateRequest;
import com.ridelink.ride.dto.FareEstimateResponse;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.dto.UpdateRideStatusRequest;
import com.ridelink.ride.dto.UserResponse;
import com.ridelink.ride.exception.InvalidStatusTransitionException;
import com.ridelink.ride.exception.NoDriversAvailableException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Location;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final FareServiceClient fareServiceClient;
    private final DriverServiceClient driverServiceClient;
    private final AccountServiceClient accountServiceClient;

    /**
     * Request a new ride:
     * 1. Validate passengerId exists and has role PASSENGER via AccountServiceClient.
     * 2. Validate pickup and destination coordinates and distance.
     * 3. Call FareServiceClient to fetch estimated fare synchronously.
     * 4. Query DriverServiceClient to check for eligible available drivers.
     * 5. If no drivers are available, throw NoDriversAvailableException.
     * 6. Save ride in MongoDB with status REQUESTED.
     */
    public RideResponse createRide(CreateRideRequest request) {
        log.info("Processing ride creation request for passengerId: {}", request.getPassengerId());

        // 1. Verify passenger exists and role equals PASSENGER
        UserResponse passenger;
        try {
            passenger = accountServiceClient.getUserById(request.getPassengerId());
        } catch (Exception ex) {
            log.warn("AccountServiceClient call failed for passengerId {}: {}", request.getPassengerId(), ex.getMessage());
            passenger = null;
        }

        if (passenger == null || passenger.getRole() == null || !passenger.getRole().equalsIgnoreCase("PASSENGER")) {
            log.error("User with ID {} is not registered as PASSENGER", request.getPassengerId());
            throw new IllegalArgumentException("Only users registered as PASSENGER can request a ride");
        }

        // 2. Validate pickup and destination coordinates and distance
        if (request.getDistanceKm() == null || request.getDistanceKm() <= 0) {
            throw new IllegalArgumentException("Distance must be greater than zero");
        }
        validateCoordinates(request.getPickupLocation(), "Pickup");
        validateCoordinates(request.getDestinationLocation(), "Destination");

        // 3. Synchronously fetch estimated fare from FareServiceClient
        Double estimatedFare = null;
        try {
            FareEstimateRequest fareRequest = FareEstimateRequest.builder()
                    .pickupLatitude(request.getPickupLocation() != null ? request.getPickupLocation().getLatitude() : null)
                    .pickupLongitude(request.getPickupLocation() != null ? request.getPickupLocation().getLongitude() : null)
                    .destinationLatitude(request.getDestinationLocation() != null ? request.getDestinationLocation().getLatitude() : null)
                    .destinationLongitude(request.getDestinationLocation() != null ? request.getDestinationLocation().getLongitude() : null)
                    .distanceKm(request.getDistanceKm())
                    .build();

            FareEstimateResponse fareResponse = fareServiceClient.estimateFare(fareRequest);
            if (fareResponse != null && fareResponse.getEstimatedFare() != null) {
                estimatedFare = fareResponse.getEstimatedFare();
                log.info("Received estimated fare from fare-service: {}", estimatedFare);
            }
        } catch (Exception ex) {
            log.warn("FareServiceClient call failed ({}). Applying default distance-based estimation.", ex.getMessage());
            // Fallback estimation: base fee 3.00 + 1.50 per km
            estimatedFare = Math.round((3.0 + (request.getDistanceKm() * 1.5)) * 100.0) / 100.0;
        }

        // 4. Query DriverServiceClient for available eligible drivers near pickup location
        List<DriverDto> availableDrivers = null;
        try {
            Double lat = request.getPickupLocation() != null ? request.getPickupLocation().getLatitude() : null;
            Double lng = request.getPickupLocation() != null ? request.getPickupLocation().getLongitude() : null;
            availableDrivers = driverServiceClient.getAvailableDrivers(lat, lng);
            log.info("DriverServiceClient returned {} available drivers", availableDrivers != null ? availableDrivers.size() : 0);
        } catch (Exception ex) {
            log.warn("DriverServiceClient call encountered an issue: {}", ex.getMessage());
        }

        // 5. Negative scenario check: If no drivers are available, throw NoDriversAvailableException
        if (availableDrivers == null || availableDrivers.isEmpty()) {
            log.warn("No available drivers found in pickup area for passenger: {}", request.getPassengerId());
            throw new NoDriversAvailableException("No eligible drivers available in this area");
        }

        // 6. Save ride in MongoDB with status REQUESTED
        Ride ride = Ride.builder()
                .passengerId(request.getPassengerId())
                .pickupLocation(request.getPickupLocation())
                .destinationLocation(request.getDestinationLocation())
                .distanceKm(request.getDistanceKm())
                .estimatedFare(estimatedFare)
                .status(RideStatus.REQUESTED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Ride savedRide = rideRepository.save(ride);
        log.info("Ride successfully created with ID: {} and status: {}", savedRide.getId(), savedRide.getStatus());

        return RideResponse.fromEntity(savedRide);
    }

    private void validateCoordinates(Location location, String type) {
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            throw new IllegalArgumentException(type + " coordinates cannot be null");
        }
        if (location.getLatitude() < -90.0 || location.getLatitude() > 90.0) {
            throw new IllegalArgumentException(type + " latitude must be between -90 and 90");
        }
        if (location.getLongitude() < -180.0 || location.getLongitude() > 180.0) {
            throw new IllegalArgumentException(type + " longitude must be between -180 and 180");
        }
    }

    /**
     * Validate and assign a driver to a ride:
     * Transition allowed strictly from REQUESTED -> ASSIGNED.
     */
    public RideResponse assignDriver(String rideId, String driverId) {
        log.info("Assigning driver {} to ride {}", driverId, rideId);

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId, true));

        if (ride.getStatus() != RideStatus.REQUESTED) {
            log.error("Cannot assign driver to ride {} because current status is {}", rideId, ride.getStatus());
            throw new InvalidStatusTransitionException(ride.getStatus(), RideStatus.ASSIGNED);
        }

        ride.setDriverId(driverId);
        ride.setStatus(RideStatus.ASSIGNED);
        ride.setUpdatedAt(Instant.now());

        Ride updatedRide = rideRepository.save(ride);
        log.info("Ride {} transitioned to ASSIGNED with driver {}", rideId, driverId);

        return RideResponse.fromEntity(updatedRide);
    }

    /**
     * Strictly enforce lifecycle transition rules:
     * - REQUESTED -> ASSIGNED or CANCELLED
     * - ASSIGNED -> ACCEPTED or CANCELLED
     * - ACCEPTED -> IN_PROGRESS or CANCELLED
     * - IN_PROGRESS -> COMPLETED
     * - COMPLETED / CANCELLED -> Terminal states (no further transitions allowed)
     * Throw InvalidStatusTransitionException if an illegal state transition is attempted.
     */
    public RideResponse updateRideStatus(String rideId, UpdateRideStatusRequest request) {
        log.info("Updating status of ride {} to {}", rideId, request.getStatus());

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId, true));

        RideStatus currentStatus = ride.getStatus();
        RideStatus targetStatus = request.getStatus();

        if (targetStatus == null) {
            throw new IllegalArgumentException("Target ride status cannot be null");
        }

        boolean isValidTransition = false;

        switch (currentStatus) {
            case REQUESTED:
                isValidTransition = (targetStatus == RideStatus.ASSIGNED || targetStatus == RideStatus.CANCELLED);
                break;
            case ASSIGNED:
                isValidTransition = (targetStatus == RideStatus.ACCEPTED || targetStatus == RideStatus.CANCELLED);
                break;
            case ACCEPTED:
                isValidTransition = (targetStatus == RideStatus.IN_PROGRESS || targetStatus == RideStatus.CANCELLED);
                break;
            case IN_PROGRESS:
                isValidTransition = (targetStatus == RideStatus.COMPLETED);
                break;
            case COMPLETED:
            case CANCELLED:
            default:
                isValidTransition = false;
                break;
        }

        if (!isValidTransition) {
            log.error("Invalid status transition attempted on ride {}: {} -> {}", rideId, currentStatus, targetStatus);
            throw new InvalidStatusTransitionException(currentStatus, targetStatus);
        }

        // Assign driverId if transitioning to ASSIGNED or ACCEPTED with driver specified
        if (request.getDriverId() != null && !request.getDriverId().isBlank()) {
            ride.setDriverId(request.getDriverId());
        }

        // Set cancellation reason when cancelling
        if (targetStatus == RideStatus.CANCELLED && request.getReason() != null) {
            ride.setCancellationReason(request.getReason());
        }

        // Finalize fare when ride completes
        if (targetStatus == RideStatus.COMPLETED && ride.getFinalFare() == null) {
            ride.setFinalFare(ride.getEstimatedFare());
        }

        ride.setStatus(targetStatus);
        ride.setUpdatedAt(Instant.now());

        Ride updatedRide = rideRepository.save(ride);
        log.info("Ride {} successfully updated to status: {}", rideId, targetStatus);

        return RideResponse.fromEntity(updatedRide);
    }

    /**
     * Return ride details by ID.
     */
    public RideResponse getRideById(String rideId) {
        log.info("Fetching ride details for ID: {}", rideId);
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId, true));
        return RideResponse.fromEntity(ride);
    }

    /**
     * Return ride history for a passenger.
     */
    public List<RideResponse> getRidesByPassenger(String passengerId) {
        log.info("Fetching ride history for passengerId: {}", passengerId);
        List<Ride> rides = rideRepository.findByPassengerId(passengerId);
        return rides.stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
