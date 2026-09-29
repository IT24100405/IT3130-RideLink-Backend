package com.ridelink.ride.controller;

import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.dto.UpdateRideStatusRequest;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
@Tag(name = "Ride Management", description = "Endpoints for managing ride lifecycle, dispatch, status updates, and passenger history")
public class RideController {

    private final RideService rideService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a ride request",
            description = "Creates a new ride request. Calls fare-service to synchronously compute estimated fare, verifies driver availability via driver-service, and persists the ride with status REQUESTED."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Ride request created successfully",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed for request payload or no available drivers"
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "No available eligible drivers found in pickup area"
            )
    })
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody CreateRideRequest request) {
        RideResponse createdRide = rideService.createRide(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRide);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Fetch ride details by ID",
            description = "Retrieves full details and current lifecycle state of a specific ride by its MongoDB ObjectId."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Ride details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ride not found with the specified ID"
            )
    })
    public ResponseEntity<RideResponse> getRideById(
            @Parameter(description = "MongoDB ObjectId of the ride", example = "64f1a2b3c4d5e6f7a8b9c0d1", required = true)
            @PathVariable("id") String id
    ) {
        RideResponse ride = rideService.getRideById(id);
        return ResponseEntity.ok(ride);
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Update ride lifecycle status",
            description = "Updates the lifecycle status of a ride following strict transition rules: ASSIGNED -> ACCEPTED, ACCEPTED -> IN_PROGRESS, IN_PROGRESS -> COMPLETED, or prior states -> CANCELLED."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Ride status updated successfully",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid lifecycle state transition attempted"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ride not found with the specified ID"
            )
    })
    public ResponseEntity<RideResponse> updateRideStatus(
            @Parameter(description = "MongoDB ObjectId of the ride", example = "64f1a2b3c4d5e6f7a8b9c0d1", required = true)
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateRideStatusRequest request
    ) {
        RideResponse updatedRide = rideService.updateRideStatus(id, request);
        return ResponseEntity.ok(updatedRide);
    }

    @PatchMapping("/{id}/assign")
    @Operation(
            summary = "Assign an available driver to a ride",
            description = "Assigns an available driver to a ride with status REQUESTED, transitioning the lifecycle state to ASSIGNED."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Driver successfully assigned to ride",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid lifecycle state transition or missing driver"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ride not found with the specified ID"
            )
    })
    public ResponseEntity<RideResponse> assignDriver(
            @Parameter(description = "MongoDB ObjectId of the ride", example = "64f1a2b3c4d5e6f7a8b9c0d1", required = true)
            @PathVariable("id") String id,
            @RequestBody UpdateRideStatusRequest request
    ) {
        RideResponse assignedRide = rideService.assignDriver(id, request.getDriverId());
        return ResponseEntity.ok(assignedRide);
    }

    @GetMapping("/passenger/{passengerId}")
    @Operation(
            summary = "List rides by passenger",
            description = "Retrieves complete ride history, estimated/final fares, and lifecycle statuses for a specific passenger ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of rides retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RideResponse.class)))
            )
    })
    public ResponseEntity<List<RideResponse>> getRidesByPassenger(
            @Parameter(description = "Unique identifier of the passenger", example = "64f1a2b3c4d5e6f7a8b9c0a1", required = true)
            @PathVariable("passengerId") String passengerId
    ) {
        List<RideResponse> rides = rideService.getRidesByPassenger(passengerId);
        return ResponseEntity.ok(rides);
    }
}
