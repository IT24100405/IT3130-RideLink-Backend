package com.ridelink.driver.controller;

import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.DriverResponse;
import com.ridelink.driver.dto.UpdateDriverStatusRequest;
import com.ridelink.driver.dto.UpdateLocationRequest;
import com.ridelink.driver.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/drivers")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Driver & Vehicle Service", description = "Endpoints for driver registration, availability status, geolocation tracking, and dispatching")
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register driver operational and vehicle details", description = "Creates a new driver profile with OFFLINE status in driver_db")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Driver successfully registered"),
            @ApiResponse(responseCode = "400", description = "Validation failed or driver profile already exists for userId")
    })
    public ResponseEntity<DriverResponse> registerDriver(@Valid @RequestBody CreateDriverRequest request) {
        log.info("REST request to register driver for userId: {}", request.getUserId());
        DriverResponse response = driverService.registerDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch driver profile by ID", description = "Retrieves full operational profile, vehicle specifications, status, and current location by driver ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Driver profile found and returned"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    public ResponseEntity<DriverResponse> getDriverById(
            @Parameter(description = "MongoDB ObjectId of the driver", required = true)
            @PathVariable("id") String id) {
        log.info("REST request to get driver by ID: {}", id);
        DriverResponse response = driverService.getDriverById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update driver availability status", description = "Updates operational status to AVAILABLE, ON_TRIP, or OFFLINE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Driver status successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status provided"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    public ResponseEntity<DriverResponse> updateStatus(
            @Parameter(description = "MongoDB ObjectId of the driver", required = true)
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateDriverStatusRequest request) {
        log.info("REST request to update status of driver ID: {} to {}", id, request.getStatus());
        DriverResponse response = driverService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/location")
    @Operation(summary = "Update simulated current location and service area", description = "Updates driver's GPS coordinates and service area")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Driver location successfully updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed for location coordinates or service area"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    public ResponseEntity<DriverResponse> updateLocation(
            @Parameter(description = "MongoDB ObjectId of the driver", required = true)
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateLocationRequest request) {
        log.info("REST request to update location of driver ID: {}", id);
        DriverResponse response = driverService.updateLocation(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    @Operation(summary = "Fetch eligible available drivers", description = "Queries drivers whose status is AVAILABLE, optionally filtered by serviceArea. Supports lat/lng coordinates for inter-service communication with ride-service.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of eligible available drivers (empty if none found)")
    })
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers(
            @Parameter(description = "Service area name (e.g. Colombo, Kandy)", required = false)
            @RequestParam(name = "serviceArea", required = false) String serviceArea,
            @Parameter(description = "Optional latitude coordinate (for inter-service geosearch)", required = false)
            @RequestParam(name = "lat", required = false) Double lat,
            @Parameter(description = "Optional longitude coordinate (for inter-service geosearch)", required = false)
            @RequestParam(name = "lng", required = false) Double lng) {
        log.info("REST request to get available drivers - serviceArea: '{}', lat: {}, lng: {}", serviceArea, lat, lng);
        List<DriverResponse> availableDrivers = (lat != null && lng != null)
                ? driverService.getAvailableDrivers(serviceArea, lat, lng)
                : driverService.getAvailableDrivers(serviceArea);
        return ResponseEntity.ok(availableDrivers);
    }
}
