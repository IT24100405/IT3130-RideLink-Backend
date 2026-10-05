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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private FareServiceClient fareServiceClient;

    @Mock
    private DriverServiceClient driverServiceClient;

    @Mock
    private AccountServiceClient accountServiceClient;

    @InjectMocks
    private RideService rideService;

    private Location pickup;
    private Location destination;
    private CreateRideRequest createRideRequest;
    private Ride sampleRide;
    private UserResponse validPassengerUser;

    @BeforeEach
    void setUp() {
        pickup = Location.builder()
                .latitude(37.7749)
                .longitude(-122.4194)
                .address("Market St, San Francisco, CA")
                .build();

        destination = Location.builder()
                .latitude(37.7891)
                .longitude(-122.4014)
                .address("Financial District, San Francisco, CA")
                .build();

        createRideRequest = CreateRideRequest.builder()
                .passengerId("passenger-101")
                .pickupLocation(pickup)
                .destinationLocation(destination)
                .distanceKm(5.5)
                .build();

        sampleRide = Ride.builder()
                .id("ride-1001")
                .passengerId("passenger-101")
                .pickupLocation(pickup)
                .destinationLocation(destination)
                .distanceKm(5.5)
                .estimatedFare(15.75)
                .status(RideStatus.REQUESTED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        validPassengerUser = UserResponse.builder()
                .id("passenger-101")
                .email("passenger@ridelink.com")
                .role("PASSENGER")
                .status("ACTIVE")
                .build();
    }

    // ==========================================
    // 1. SUCCESS SCENARIO
    // ==========================================

    @Test
    @DisplayName("Success Scenario: Creating a ride successfully when passenger is valid, fare estimate succeeds and drivers are available")
    void createRide_Success_WhenDriversAvailableAndFareEstimateSucceeds() {
        // Arrange
        FareEstimateResponse fareResponse = FareEstimateResponse.builder()
                .estimatedFare(15.75)
                .build();

        DriverDto availableDriver = DriverDto.builder()
                .driverId("driver-201")
                .fullName("Alex Driver")
                .vehicleNumber("SF-4455")
                .isAvailable(true)
                .build();

        when(accountServiceClient.getUserById("passenger-101")).thenReturn(validPassengerUser);
        when(fareServiceClient.estimateFare(any(FareEstimateRequest.class))).thenReturn(fareResponse);
        when(driverServiceClient.getAvailableDrivers(anyDouble(), anyDouble())).thenReturn(List.of(availableDriver));
        when(rideRepository.save(any(Ride.class))).thenReturn(sampleRide);

        // Act
        RideResponse response = rideService.createRide(createRideRequest);

        // Assert
        assertNotNull(response);
        assertEquals("ride-1001", response.getId());
        assertEquals("passenger-101", response.getPassengerId());
        assertEquals(RideStatus.REQUESTED, response.getStatus());
        assertEquals(15.75, response.getEstimatedFare());
        assertEquals(5.5, response.getDistanceKm());

        verify(accountServiceClient, times(1)).getUserById("passenger-101");
        verify(fareServiceClient, times(1)).estimateFare(any(FareEstimateRequest.class));
        verify(driverServiceClient, times(1)).getAvailableDrivers(37.7749, -122.4194);
        verify(rideRepository, times(1)).save(any(Ride.class));
    }

    // ==========================================
    // 2. PASSENGER & COORDINATE VALIDATION TESTS
    // ==========================================

    @Test
    @DisplayName("Validation Test: Rejects ride creation when user role is not PASSENGER")
    void createRide_ThrowsIllegalArgumentException_WhenUserNotPassenger() {
        UserResponse driverUser = UserResponse.builder()
                .id("passenger-101")
                .role("DRIVER")
                .build();
        when(accountServiceClient.getUserById("passenger-101")).thenReturn(driverUser);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                rideService.createRide(createRideRequest)
        );

        assertEquals("Only users registered as PASSENGER can request a ride", ex.getMessage());
        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validation Test: Rejects ride creation when passenger not found")
    void createRide_ThrowsIllegalArgumentException_WhenPassengerNotFound() {
        when(accountServiceClient.getUserById("passenger-101")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                rideService.createRide(createRideRequest)
        );

        assertEquals("Only users registered as PASSENGER can request a ride", ex.getMessage());
        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validation Test: Rejects invalid latitude or longitude coordinates")
    void createRide_ThrowsIllegalArgumentException_WhenCoordinatesInvalid() {
        when(accountServiceClient.getUserById("passenger-101")).thenReturn(validPassengerUser);

        createRideRequest.getPickupLocation().setLatitude(95.0); // Invalid lat > 90

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                rideService.createRide(createRideRequest)
        );

        assertTrue(ex.getMessage().contains("latitude must be between -90 and 90"));

        createRideRequest.getPickupLocation().setLatitude(37.7749);
        createRideRequest.getDestinationLocation().setLongitude(-190.0); // Invalid lng < -180

        ex = assertThrows(IllegalArgumentException.class, () ->
                rideService.createRide(createRideRequest)
        );

        assertTrue(ex.getMessage().contains("longitude must be between -180 and 180"));
    }

    @Test
    @DisplayName("Validation Test: Rejects non-positive distance")
    void createRide_ThrowsIllegalArgumentException_WhenDistanceNonPositive() {
        when(accountServiceClient.getUserById("passenger-101")).thenReturn(validPassengerUser);
        createRideRequest.setDistanceKm(0.0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                rideService.createRide(createRideRequest)
        );

        assertTrue(ex.getMessage().contains("Distance must be greater than zero"));
    }

    // ==========================================
    // 3. LIFECYCLE TRANSITION SCENARIOS
    // ==========================================

    @Test
    @DisplayName("Lifecycle Transition: Transitioning status from REQUESTED to ASSIGNED")
    void assignDriver_Success_WhenStatusIsRequested() {
        // Arrange
        when(rideRepository.findById("ride-1001")).thenReturn(Optional.of(sampleRide));

        Ride assignedRide = Ride.builder()
                .id("ride-1001")
                .passengerId("passenger-101")
                .driverId("driver-201")
                .pickupLocation(pickup)
                .destinationLocation(destination)
                .distanceKm(5.5)
                .estimatedFare(15.75)
                .status(RideStatus.ASSIGNED)
                .createdAt(sampleRide.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        when(rideRepository.save(any(Ride.class))).thenReturn(assignedRide);

        // Act
        RideResponse response = rideService.assignDriver("ride-1001", "driver-201");

        // Assert
        assertNotNull(response);
        assertEquals(RideStatus.ASSIGNED, response.getStatus());
        assertEquals("driver-201", response.getDriverId());

        verify(rideRepository, times(1)).findById("ride-1001");
        verify(rideRepository, times(1)).save(any(Ride.class));
    }

    @Test
    @DisplayName("Lifecycle Transition: Transitioning status from IN_PROGRESS to COMPLETED")
    void updateRideStatus_Success_FromInProgressToCompleted() {
        // Arrange
        Ride inProgressRide = Ride.builder()
                .id("ride-1001")
                .passengerId("passenger-101")
                .driverId("driver-201")
                .pickupLocation(pickup)
                .destinationLocation(destination)
                .distanceKm(5.5)
                .estimatedFare(15.75)
                .status(RideStatus.IN_PROGRESS)
                .createdAt(sampleRide.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        when(rideRepository.findById("ride-1001")).thenReturn(Optional.of(inProgressRide));

        Ride completedRide = Ride.builder()
                .id("ride-1001")
                .passengerId("passenger-101")
                .driverId("driver-201")
                .pickupLocation(pickup)
                .destinationLocation(destination)
                .distanceKm(5.5)
                .estimatedFare(15.75)
                .finalFare(15.75)
                .status(RideStatus.COMPLETED)
                .createdAt(sampleRide.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        when(rideRepository.save(any(Ride.class))).thenReturn(completedRide);

        UpdateRideStatusRequest request = UpdateRideStatusRequest.builder()
                .status(RideStatus.COMPLETED)
                .build();

        // Act
        RideResponse response = rideService.updateRideStatus("ride-1001", request);

        // Assert
        assertNotNull(response);
        assertEquals(RideStatus.COMPLETED, response.getStatus());
        assertEquals(15.75, response.getFinalFare());

        verify(rideRepository, times(1)).findById("ride-1001");
        verify(rideRepository, times(1)).save(any(Ride.class));
    }

    @Test
    @DisplayName("Lifecycle Transition: Transitioning status from ASSIGNED to ACCEPTED and ACCEPTED to IN_PROGRESS")
    void updateRideStatus_Success_ThroughIntermediateLifecycleStates() {
        // ASSIGNED -> ACCEPTED
        sampleRide.setStatus(RideStatus.ASSIGNED);
        sampleRide.setDriverId("driver-201");
        when(rideRepository.findById("ride-1001")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateRideStatusRequest acceptRequest = UpdateRideStatusRequest.builder()
                .status(RideStatus.ACCEPTED)
                .build();

        RideResponse acceptedResponse = rideService.updateRideStatus("ride-1001", acceptRequest);
        assertEquals(RideStatus.ACCEPTED, acceptedResponse.getStatus());

        // ACCEPTED -> IN_PROGRESS
        sampleRide.setStatus(RideStatus.ACCEPTED);
        UpdateRideStatusRequest startRequest = UpdateRideStatusRequest.builder()
                .status(RideStatus.IN_PROGRESS)
                .build();

        RideResponse inProgressResponse = rideService.updateRideStatus("ride-1001", startRequest);
        assertEquals(RideStatus.IN_PROGRESS, inProgressResponse.getStatus());
    }

    @Test
    @DisplayName("Lifecycle Transition: Transitioning status to CANCELLED from REQUESTED, ASSIGNED, or ACCEPTED")
    void updateRideStatus_Success_CancellationPriorToInProgress() {
        // REQUESTED -> CANCELLED
        sampleRide.setStatus(RideStatus.REQUESTED);
        when(rideRepository.findById("ride-1001")).thenReturn(Optional.of(sampleRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateRideStatusRequest cancelRequest = UpdateRideStatusRequest.builder()
                .status(RideStatus.CANCELLED)
                .reason("Changed plans")
                .build();

        RideResponse response = rideService.updateRideStatus("ride-1001", cancelRequest);
        assertNotNull(response);
        assertEquals(RideStatus.CANCELLED, response.getStatus());
        assertEquals("Changed plans", response.getCancellationReason());

        // ASSIGNED -> CANCELLED
        sampleRide.setStatus(RideStatus.ASSIGNED);
        RideResponse assignedCancel = rideService.updateRideStatus("ride-1001", cancelRequest);
        assertEquals(RideStatus.CANCELLED, assignedCancel.getStatus());

        // ACCEPTED -> CANCELLED
        sampleRide.setStatus(RideStatus.ACCEPTED);
        RideResponse acceptedCancel = rideService.updateRideStatus("ride-1001", cancelRequest);
        assertEquals(RideStatus.CANCELLED, acceptedCancel.getStatus());
    }

    // ==========================================
    // 4. NEGATIVE SCENARIOS
    // ==========================================

    @Test
    @DisplayName("Negative Scenario 1: Attempting to create a ride when DriverServiceClient returns an empty list, verifying NoDriversAvailableException is thrown")
    void createRide_ThrowsNoDriversAvailableException_WhenDriversListIsEmpty() {
        // Arrange
        FareEstimateResponse fareResponse = FareEstimateResponse.builder()
                .estimatedFare(15.75)
                .build();

        when(accountServiceClient.getUserById("passenger-101")).thenReturn(validPassengerUser);
        when(fareServiceClient.estimateFare(any(FareEstimateRequest.class))).thenReturn(fareResponse);
        when(driverServiceClient.getAvailableDrivers(anyDouble(), anyDouble())).thenReturn(Collections.emptyList());

        // Act & Assert
        NoDriversAvailableException exception = assertThrows(
                NoDriversAvailableException.class,
                () -> rideService.createRide(createRideRequest)
        );

        assertTrue(exception.getMessage().contains("No eligible drivers available in this area"));
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    @DisplayName("Negative Scenario 1 (Variant): Attempting to create a ride when DriverServiceClient returns null")
    void createRide_ThrowsNoDriversAvailableException_WhenDriversListIsNull() {
        FareEstimateResponse fareResponse = FareEstimateResponse.builder()
                .estimatedFare(15.75)
                .build();

        when(accountServiceClient.getUserById("passenger-101")).thenReturn(validPassengerUser);
        when(fareServiceClient.estimateFare(any(FareEstimateRequest.class))).thenReturn(fareResponse);
        when(driverServiceClient.getAvailableDrivers(anyDouble(), anyDouble())).thenReturn(null);

        assertThrows(NoDriversAvailableException.class, () -> rideService.createRide(createRideRequest));
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    @DisplayName("Negative Scenario 2: Attempting an invalid status jump (e.g., REQUESTED directly to COMPLETED), verifying InvalidStatusTransitionException is thrown")
    void updateRideStatus_ThrowsInvalidStatusTransitionException_WhenDirectJumpToCompletedAttempted() {
        sampleRide.setStatus(RideStatus.REQUESTED);
        when(rideRepository.findById("ride-1001")).thenReturn(Optional.of(sampleRide));

        UpdateRideStatusRequest invalidRequest = UpdateRideStatusRequest.builder()
                .status(RideStatus.COMPLETED)
                .build();

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> rideService.updateRideStatus("ride-1001", invalidRequest)
        );

        assertTrue(exception.getMessage().contains("Invalid ride status transition"));
        assertTrue(exception.getMessage().contains("Cannot transition ride from REQUESTED to COMPLETED"));
        assertEquals(RideStatus.REQUESTED, exception.getCurrentStatus());
        assertEquals(RideStatus.COMPLETED, exception.getTargetStatus());

        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    @DisplayName("Negative Scenario 2 (Variant): Attempting to assign a driver when ride is not in REQUESTED status")
    void assignDriver_ThrowsInvalidStatusTransitionException_WhenCurrentStatusIsNotRequested() {
        sampleRide.setStatus(RideStatus.IN_PROGRESS);
        when(rideRepository.findById("ride-1001")).thenReturn(Optional.of(sampleRide));

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> rideService.assignDriver("ride-1001", "driver-201")
        );

        assertTrue(exception.getMessage().contains("Invalid ride status transition"));
        assertTrue(exception.getMessage().contains("Cannot transition ride from IN_PROGRESS to ASSIGNED"));
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    @DisplayName("Negative Scenario 2 (Variant): Attempting to cancel an IN_PROGRESS ride (only COMPLETED allowed)")
    void updateRideStatus_ThrowsInvalidStatusTransitionException_WhenCancellingInProgressRide() {
        sampleRide.setStatus(RideStatus.IN_PROGRESS);
        when(rideRepository.findById("ride-1001")).thenReturn(Optional.of(sampleRide));

        UpdateRideStatusRequest cancelRequest = UpdateRideStatusRequest.builder()
                .status(RideStatus.CANCELLED)
                .build();

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> rideService.updateRideStatus("ride-1001", cancelRequest)
        );

        assertTrue(exception.getMessage().contains("Invalid ride status transition"));
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    @DisplayName("Negative Scenario 2 (Variant): Terminal state - Attempting transition from COMPLETED")
    void updateRideStatus_ThrowsInvalidStatusTransitionException_WhenTransitionFromCompleted() {
        sampleRide.setStatus(RideStatus.COMPLETED);
        when(rideRepository.findById("ride-1001")).thenReturn(Optional.of(sampleRide));

        UpdateRideStatusRequest cancelRequest = UpdateRideStatusRequest.builder()
                .status(RideStatus.CANCELLED)
                .reason("Customer disputed")
                .build();

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> rideService.updateRideStatus("ride-1001", cancelRequest)
        );

        assertTrue(exception.getMessage().contains("Invalid ride status transition"));
        assertTrue(exception.getMessage().contains("Cannot transition ride from COMPLETED to CANCELLED"));
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    @DisplayName("Negative Scenario 2 (Variant): Terminal state - Attempting transition from CANCELLED")
    void updateRideStatus_ThrowsInvalidStatusTransitionException_WhenTransitionFromCancelled() {
        sampleRide.setStatus(RideStatus.CANCELLED);
        when(rideRepository.findById("ride-1001")).thenReturn(Optional.of(sampleRide));

        UpdateRideStatusRequest req = UpdateRideStatusRequest.builder()
                .status(RideStatus.ASSIGNED)
                .build();

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> rideService.updateRideStatus("ride-1001", req)
        );

        assertTrue(exception.getMessage().contains("Invalid ride status transition"));
        verify(rideRepository, never()).save(any(Ride.class));
    }

    // ==========================================
    // 5. QUERY SCENARIOS
    // ==========================================

    @Test
    @DisplayName("Query Scenario: Fetching ride by ID succeeds")
    void getRideById_Success() {
        when(rideRepository.findById("ride-1001")).thenReturn(Optional.of(sampleRide));

        RideResponse response = rideService.getRideById("ride-1001");

        assertNotNull(response);
        assertEquals("ride-1001", response.getId());
    }

    @Test
    @DisplayName("Query Scenario: Fetching non-existent ride by ID throws RideNotFoundException")
    void getRideById_ThrowsRideNotFoundException_WhenNotFound() {
        when(rideRepository.findById("ride-9999")).thenReturn(Optional.empty());

        assertThrows(RideNotFoundException.class, () -> rideService.getRideById("ride-9999"));
    }

    @Test
    @DisplayName("Query Scenario: Fetching rides by passenger ID returns list")
    void getRidesByPassenger_Success() {
        when(rideRepository.findByPassengerId("passenger-101")).thenReturn(List.of(sampleRide));

        List<RideResponse> responses = rideService.getRidesByPassenger("passenger-101");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("passenger-101", responses.get(0).getPassengerId());
    }
}
