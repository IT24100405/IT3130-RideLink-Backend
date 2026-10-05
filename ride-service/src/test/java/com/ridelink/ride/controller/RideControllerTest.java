package com.ridelink.ride.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.dto.UpdateRideStatusRequest;
import com.ridelink.ride.exception.GlobalExceptionHandler;
import com.ridelink.ride.exception.InvalidStatusTransitionException;
import com.ridelink.ride.exception.NoDriversAvailableException;
import com.ridelink.ride.exception.ResourceNotFoundException;
import com.ridelink.ride.model.Location;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.service.RideService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RideControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RideService rideService;

    @InjectMocks
    private RideController rideController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rideController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Validation Error: Create ride with invalid/null fields returns 400 Bad Request with field errors")
    void createRide_ValidationError_Returns400() throws Exception {
        CreateRideRequest invalidRequest = CreateRideRequest.builder()
                .passengerId("") // blank
                .pickupLocation(null) // null
                .destinationLocation(null) // null
                .distanceKm(-5.0) // negative
                .build();

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.details.pickupLocation").exists())
                .andExpect(jsonPath("$.details.distanceKm").exists());
    }

    @Test
    @DisplayName("Negative Scenario: Create ride when passenger role is not PASSENGER returns 400 Bad Request")
    void createRide_NotPassengerRole_Returns400() throws Exception {
        CreateRideRequest request = CreateRideRequest.builder()
                .passengerId("driver-user-id")
                .pickupLocation(new Location(37.7749, -122.4194, "Market St"))
                .destinationLocation(new Location(37.7891, -122.4014, "Financial District"))
                .distanceKm(5.0)
                .build();

        when(rideService.createRide(any(CreateRideRequest.class)))
                .thenThrow(new IllegalArgumentException("Only users registered as PASSENGER can request a ride"));

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Only users registered as PASSENGER can request a ride"));
    }

    @Test
    @DisplayName("Negative Scenario: Create ride when no drivers available returns 404 Not Found")
    void createRide_NoDriversAvailable_Returns404() throws Exception {
        CreateRideRequest request = CreateRideRequest.builder()
                .passengerId("p1")
                .pickupLocation(new Location(37.7749, -122.4194, "Market St"))
                .destinationLocation(new Location(37.7891, -122.4014, "Financial District"))
                .distanceKm(5.0)
                .build();

        when(rideService.createRide(any(CreateRideRequest.class)))
                .thenThrow(new NoDriversAvailableException("No eligible drivers available in this area"));

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No Drivers Available"))
                .andExpect(jsonPath("$.message").value("No eligible drivers available in this area"));
    }

    @Test
    @DisplayName("Negative Scenario: Illegal ride status transition returns 400 Bad Request")
    void updateRideStatus_IllegalTransition_Returns400() throws Exception {
        UpdateRideStatusRequest request = UpdateRideStatusRequest.builder()
                .status(RideStatus.COMPLETED)
                .build();

        when(rideService.updateRideStatus(eq("ride-1"), any(UpdateRideStatusRequest.class)))
                .thenThrow(new InvalidStatusTransitionException(RideStatus.REQUESTED, RideStatus.COMPLETED));

        mockMvc.perform(patch("/api/rides/ride-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid ride status transition: Cannot transition ride from REQUESTED to COMPLETED"));
    }

    @Test
    @DisplayName("Negative Scenario: Fetching non-existent ride returns 404 Not Found")
    void getRideById_NotFound_Returns404() throws Exception {
        when(rideService.getRideById("nonexistent"))
                .thenThrow(new ResourceNotFoundException("Ride not found with id: nonexistent"));

        mockMvc.perform(get("/api/rides/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ride not found with id: nonexistent"));
    }

    @Test
    @DisplayName("Success Scenario: Create ride successfully returns 201 Created")
    void createRide_Success_Returns201() throws Exception {
        CreateRideRequest request = CreateRideRequest.builder()
                .passengerId("p1")
                .pickupLocation(new Location(37.7749, -122.4194, "Market St"))
                .destinationLocation(new Location(37.7891, -122.4014, "Financial District"))
                .distanceKm(5.0)
                .build();

        RideResponse response = RideResponse.builder()
                .id("ride-101")
                .passengerId("p1")
                .status(RideStatus.REQUESTED)
                .estimatedFare(15.5)
                .distanceKm(5.0)
                .build();

        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ride-101"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.estimatedFare").value(15.5));
    }
}
