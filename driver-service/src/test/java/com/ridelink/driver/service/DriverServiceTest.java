package com.ridelink.driver.service;

import com.ridelink.driver.client.AccountServiceClient;

import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.DriverResponse;
import com.ridelink.driver.dto.UpdateLocationRequest;
import com.ridelink.driver.dto.UserResponse;
import com.ridelink.driver.dto.VehicleDto;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.model.Driver;
import com.ridelink.driver.model.DriverStatus;
import com.ridelink.driver.model.Location;
import com.ridelink.driver.model.Vehicle;
import com.ridelink.driver.model.VehicleType;
import com.ridelink.driver.repository.DriverRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @InjectMocks
    private DriverService driverService;

    private CreateDriverRequest createDriverRequest;
    private Driver sampleDriver;
    private Location sampleLocation;
    private Vehicle sampleVehicle;
    private UserResponse validDriverUser;

    @BeforeEach
    void setUp() {
        VehicleDto vehicleDto = VehicleDto.builder()
                .vehicleNumber("CAB-1234")
                .model("Toyota Prius")
                .vehicleType(VehicleType.CAR)
                .color("White")
                .build();

        createDriverRequest = CreateDriverRequest.builder()
                .userId("user-101")
                .fullName("John Silva")
                .phoneNumber("+94771234567")
                .licenseNumber("B1234567")
                .vehicle(vehicleDto)
                .build();

        validDriverUser = UserResponse.builder()
                .id("user-101")
                .email("john.driver@example.com")
                .role("DRIVER")
                .build();

        sampleVehicle = Vehicle.builder()
                .vehicleNumber("CAB-1234")
                .model("Toyota Prius")
                .vehicleType(VehicleType.CAR)
                .color("White")
                .build();

        sampleLocation = Location.builder()
                .latitude(6.9271)
                .longitude(79.8612)
                .serviceArea("Colombo")
                .build();

        sampleDriver = Driver.builder()
                .id("driver-1001")
                .userId("user-101")
                .fullName("John Silva")
                .phoneNumber("+94771234567")
                .licenseNumber("B1234567")
                .vehicle(sampleVehicle)
                .status(DriverStatus.OFFLINE)
                .currentLocation(sampleLocation)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // SUCCESS TEST: Registering a Driver
    // =========================================================================
    @Test
    @DisplayName("Success Test: Registering a driver profile with valid details sets status to OFFLINE")
    void registerDriver_Success_WithValidDetails() {
        when(accountServiceClient.getUserById("user-101")).thenReturn(validDriverUser);
        when(driverRepository.findByUserId("user-101")).thenReturn(Optional.empty());
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> {
            Driver driverArg = invocation.getArgument(0);
            driverArg.setId("driver-1001");
            driverArg.setCreatedAt(Instant.now());
            driverArg.setUpdatedAt(Instant.now());
            return driverArg;
        });

        DriverResponse response = driverService.registerDriver(createDriverRequest);

        assertNotNull(response);
        assertEquals("driver-1001", response.getId());
        assertEquals("user-101", response.getUserId());
        assertEquals("John Silva", response.getFullName());
        assertEquals("+94771234567", response.getPhoneNumber());
        assertEquals("B1234567", response.getLicenseNumber());
        assertEquals(DriverStatus.OFFLINE, response.getStatus());
        assertFalse(response.getIsAvailable());
        assertNotNull(response.getVehicle());
        assertEquals("CAB-1234", response.getVehicle().getVehicleNumber());
        assertEquals(VehicleType.CAR, response.getVehicle().getVehicleType());

        verify(accountServiceClient, times(1)).getUserById("user-101");
        verify(driverRepository, times(1)).findByUserId("user-101");
        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    @Test
    @DisplayName("Negative Test: Attempting to register a driver with non-DRIVER role throws IllegalArgumentException")
    void registerDriver_ThrowsIllegalArgumentException_WhenUserNotDriverRole() {
        UserResponse passengerUser = UserResponse.builder()
                .id("user-101")
                .email("passenger@example.com")
                .role("PASSENGER")
                .build();
        when(accountServiceClient.getUserById("user-101")).thenReturn(passengerUser);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                driverService.registerDriver(createDriverRequest)
        );

        assertEquals("Only users registered as DRIVER can create a driver profile", exception.getMessage());
        verify(accountServiceClient, times(1)).getUserById("user-101");
        verify(driverRepository, never()).findByUserId(anyString());
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    @DisplayName("Negative Test: Attempting to register a driver when user not found in account-service throws IllegalArgumentException")
    void registerDriver_ThrowsIllegalArgumentException_WhenUserNotFoundInAccountService() {
        when(accountServiceClient.getUserById("user-101")).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                driverService.registerDriver(createDriverRequest)
        );

        assertEquals("Only users registered as DRIVER can create a driver profile", exception.getMessage());
        verify(accountServiceClient, times(1)).getUserById("user-101");
        verify(driverRepository, never()).findByUserId(anyString());
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    @DisplayName("Negative Test: Registering a driver with an existing userId throws IllegalArgumentException")
    void registerDriver_ThrowsIllegalArgumentException_WhenUserAlreadyExists() {
        when(accountServiceClient.getUserById("user-101")).thenReturn(validDriverUser);
        when(driverRepository.findByUserId("user-101")).thenReturn(Optional.of(sampleDriver));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                driverService.registerDriver(createDriverRequest)
        );

        assertTrue(exception.getMessage().contains("Driver profile already exists for userId: user-101"));
        verify(accountServiceClient, times(1)).getUserById("user-101");
        verify(driverRepository, times(1)).findByUserId("user-101");
        verify(driverRepository, never()).save(any(Driver.class));
    }


    // =========================================================================
    // STATUS UPDATE TEST: Updating Driver Status
    // =========================================================================
    @Test
    @DisplayName("Status Update Test: Updating driver availability status to AVAILABLE")
    void updateStatus_Success_ToAvailable() {
        when(driverRepository.findById("driver-1001")).thenReturn(Optional.of(sampleDriver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriverResponse response = driverService.updateStatus("driver-1001", DriverStatus.AVAILABLE);

        assertNotNull(response);
        assertEquals("driver-1001", response.getId());
        assertEquals(DriverStatus.AVAILABLE, response.getStatus());
        assertTrue(response.getIsAvailable());

        verify(driverRepository, times(1)).findById("driver-1001");
        verify(driverRepository, times(1)).save(argThat(d -> d.getStatus() == DriverStatus.AVAILABLE));
    }

    @Test
    @DisplayName("Status Update Test: Updating driver availability status to ON_TRIP and OFFLINE")
    void updateStatus_Success_ToOnTripAndOffline() {
        when(driverRepository.findById("driver-1001")).thenReturn(Optional.of(sampleDriver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriverResponse onTripResponse = driverService.updateStatus("driver-1001", DriverStatus.ON_TRIP);
        assertEquals(DriverStatus.ON_TRIP, onTripResponse.getStatus());
        assertFalse(onTripResponse.getIsAvailable());

        DriverResponse offlineResponse = driverService.updateStatus("driver-1001", DriverStatus.OFFLINE);
        assertEquals(DriverStatus.OFFLINE, offlineResponse.getStatus());
        assertFalse(offlineResponse.getIsAvailable());

        verify(driverRepository, times(2)).findById("driver-1001");
        verify(driverRepository, times(2)).save(any(Driver.class));
    }

    // =========================================================================
    // LOCATION UPDATE TEST: Updating Driver Location
    // =========================================================================
    @Test
    @DisplayName("Location Update Test: Updating simulated current location and service area")
    void updateLocation_Success() {
        UpdateLocationRequest updateLocationRequest = UpdateLocationRequest.builder()
                .latitude(6.9319)
                .longitude(79.8478)
                .serviceArea("Colombo Fort")
                .build();

        when(driverRepository.findById("driver-1001")).thenReturn(Optional.of(sampleDriver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriverResponse response = driverService.updateLocation("driver-1001", updateLocationRequest);

        assertNotNull(response);
        assertNotNull(response.getCurrentLocation());
        assertEquals(6.9319, response.getCurrentLocation().getLatitude());
        assertEquals(79.8478, response.getCurrentLocation().getLongitude());
        assertEquals("Colombo Fort", response.getCurrentLocation().getServiceArea());

        verify(driverRepository, times(1)).findById("driver-1001");
        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    // =========================================================================
    // RETRIEVAL TEST: Fetching Available Drivers
    // =========================================================================
    @Test
    @DisplayName("Retrieval Test: Fetching available drivers filtered by service area")
    void getAvailableDrivers_FilteredByServiceArea_Success() {
        Driver availableDriver = Driver.builder()
                .id("driver-1002")
                .userId("user-102")
                .fullName("Kamal Perera")
                .phoneNumber("+94777654321")
                .licenseNumber("B7654321")
                .vehicle(sampleVehicle)
                .status(DriverStatus.AVAILABLE)
                .currentLocation(Location.builder().latitude(6.9271).longitude(79.8612).serviceArea("Colombo").build())
                .build();

        when(driverRepository.findByStatusAndCurrentLocation_ServiceArea(DriverStatus.AVAILABLE, "Colombo"))
                .thenReturn(List.of(availableDriver));

        List<DriverResponse> result = driverService.getAvailableDrivers("Colombo");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("driver-1002", result.get(0).getId());
        assertEquals("Kamal Perera", result.get(0).getFullName());
        assertEquals(DriverStatus.AVAILABLE, result.get(0).getStatus());
        assertTrue(result.get(0).getIsAvailable());
        assertEquals("Colombo", result.get(0).getCurrentLocation().getServiceArea());

        verify(driverRepository, times(1)).findByStatusAndCurrentLocation_ServiceArea(DriverStatus.AVAILABLE, "Colombo");
        verify(driverRepository, never()).findByStatus(any());
    }

    @Test
    @DisplayName("Retrieval Test: Fetching all available drivers when serviceArea is null or empty")
    void getAvailableDrivers_WhenServiceAreaNullOrEmpty_ReturnsAllAvailableDrivers() {
        Driver availableDriver1 = Driver.builder()
                .id("driver-1")
                .status(DriverStatus.AVAILABLE)
                .vehicle(sampleVehicle)
                .currentLocation(Location.builder().serviceArea("Colombo").build())
                .build();

        Driver availableDriver2 = Driver.builder()
                .id("driver-2")
                .status(DriverStatus.AVAILABLE)
                .vehicle(sampleVehicle)
                .currentLocation(Location.builder().serviceArea("Kandy").build())
                .build();

        when(driverRepository.findByStatus(DriverStatus.AVAILABLE)).thenReturn(List.of(availableDriver1, availableDriver2));

        List<DriverResponse> nullAreaResult = driverService.getAvailableDrivers(null);
        assertEquals(2, nullAreaResult.size());

        List<DriverResponse> emptyAreaResult = driverService.getAvailableDrivers("   ");
        assertEquals(2, emptyAreaResult.size());

        verify(driverRepository, times(2)).findByStatus(DriverStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Retrieval Test: Filters out drivers without registered vehicle details")
    void getAvailableDrivers_FiltersOutDriversWithoutVehicles() {
        Driver availableWithVehicle = Driver.builder()
                .id("driver-1")
                .status(DriverStatus.AVAILABLE)
                .vehicle(sampleVehicle)
                .build();

        Driver availableNoVehicle = Driver.builder()
                .id("driver-2")
                .status(DriverStatus.AVAILABLE)
                .vehicle(null)
                .build();

        Driver availableEmptyPlate = Driver.builder()
                .id("driver-3")
                .status(DriverStatus.AVAILABLE)
                .vehicle(Vehicle.builder().vehicleNumber("").build())
                .build();

        when(driverRepository.findByStatus(DriverStatus.AVAILABLE))
                .thenReturn(List.of(availableWithVehicle, availableNoVehicle, availableEmptyPlate));

        List<DriverResponse> result = driverService.getAvailableDrivers(null);

        assertEquals(1, result.size());
        assertEquals("driver-1", result.get(0).getId());
    }

    @Test
    @DisplayName("Retrieval Test: Returns empty list when no drivers are available (negative scenario evaluation)")
    void getAvailableDrivers_ReturnsEmptyList_WhenNoDriversFound() {
        when(driverRepository.findByStatusAndCurrentLocation_ServiceArea(DriverStatus.AVAILABLE, "Galle"))
                .thenReturn(Collections.emptyList());

        List<DriverResponse> result = driverService.getAvailableDrivers("Galle");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(driverRepository, times(1)).findByStatusAndCurrentLocation_ServiceArea(DriverStatus.AVAILABLE, "Galle");
    }

    // =========================================================================
    // RETRIEVAL TEST: Fetching Driver By ID
    // =========================================================================
    @Test
    @DisplayName("Retrieval Test: Fetching driver profile by valid ID")
    void getDriverById_Success() {
        when(driverRepository.findById("driver-1001")).thenReturn(Optional.of(sampleDriver));

        DriverResponse response = driverService.getDriverById("driver-1001");

        assertNotNull(response);
        assertEquals("driver-1001", response.getId());
        assertEquals("John Silva", response.getFullName());
        assertEquals("user-101", response.getUserId());
        verify(driverRepository, times(1)).findById("driver-1001");
    }

    // =========================================================================
    // NEGATIVE TESTS: ResourceNotFoundException Assertions
    // =========================================================================
    @Test
    @DisplayName("Negative Test: Attempting to retrieve a driver with a non-existent ID asserts ResourceNotFoundException")
    void getDriverById_ThrowsResourceNotFoundException_WhenDriverNotFound() {
        when(driverRepository.findById("non-existent-id")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                driverService.getDriverById("non-existent-id")
        );

        assertEquals("Driver not found with id: non-existent-id", exception.getMessage());
        verify(driverRepository, times(1)).findById("non-existent-id");
    }

    @Test
    @DisplayName("Negative Test: Attempting to update status with a non-existent ID asserts ResourceNotFoundException")
    void updateStatus_ThrowsResourceNotFoundException_WhenDriverNotFound() {
        when(driverRepository.findById("non-existent-id")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                driverService.updateStatus("non-existent-id", DriverStatus.AVAILABLE)
        );

        assertEquals("Driver not found with id: non-existent-id", exception.getMessage());
        verify(driverRepository, times(1)).findById("non-existent-id");
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    @DisplayName("Negative Test: Attempting to update location with a non-existent ID asserts ResourceNotFoundException")
    void updateLocation_ThrowsResourceNotFoundException_WhenDriverNotFound() {
        UpdateLocationRequest request = UpdateLocationRequest.builder()
                .latitude(6.9271)
                .longitude(79.8612)
                .serviceArea("Colombo")
                .build();

        when(driverRepository.findById("non-existent-id")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                driverService.updateLocation("non-existent-id", request)
        );

        assertEquals("Driver not found with id: non-existent-id", exception.getMessage());
        verify(driverRepository, times(1)).findById("non-existent-id");
        verify(driverRepository, never()).save(any(Driver.class));
    }
}
