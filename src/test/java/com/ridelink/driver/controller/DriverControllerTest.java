package com.ridelink.driver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.DriverResponse;
import com.ridelink.driver.dto.VehicleDto;
import com.ridelink.driver.exception.GlobalExceptionHandler;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.model.DriverStatus;
import com.ridelink.driver.model.VehicleType;
import com.ridelink.driver.service.DriverService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DriverControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DriverService driverService;

    @InjectMocks
    private DriverController driverController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(driverController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Validation Error: Register driver with blank fields returns 400 Bad Request with structured field errors")
    void registerDriver_ValidationError_Returns400WithDetails() throws Exception {
        CreateDriverRequest invalidRequest = CreateDriverRequest.builder()
                .userId("") // blank
                .fullName("") // blank
                .phoneNumber("") // blank
                .licenseNumber("") // blank
                .vehicle(null) // null vehicle
                .build();

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.details.userId").exists())
                .andExpect(jsonPath("$.details.vehicle").exists());
    }

    @Test
    @DisplayName("Negative Scenario: Register driver with non-DRIVER role returns 400 Bad Request")
    void registerDriver_NotDriverRole_Returns400() throws Exception {
        VehicleDto vehicle = VehicleDto.builder()
                .vehicleNumber("CAB-1234")
                .model("Toyota Prius")
                .vehicleType(VehicleType.CAR)
                .build();

        CreateDriverRequest request = CreateDriverRequest.builder()
                .userId("passenger-user")
                .fullName("John Doe")
                .phoneNumber("+94771234567")
                .licenseNumber("B1234567")
                .vehicle(vehicle)
                .build();

        when(driverService.registerDriver(any(CreateDriverRequest.class)))
                .thenThrow(new IllegalArgumentException("Only users registered as DRIVER can create a driver profile"));

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Only users registered as DRIVER can create a driver profile"));
    }

    @Test
    @DisplayName("Negative Scenario: Duplicate driver profile creation returns 400 Bad Request")
    void registerDriver_DuplicateProfile_Returns400() throws Exception {
        VehicleDto vehicle = VehicleDto.builder()
                .vehicleNumber("CAB-1234")
                .model("Toyota Prius")
                .vehicleType(VehicleType.CAR)
                .build();

        CreateDriverRequest request = CreateDriverRequest.builder()
                .userId("existing-driver")
                .fullName("John Silva")
                .phoneNumber("+94771234567")
                .licenseNumber("B1234567")
                .vehicle(vehicle)
                .build();

        when(driverService.registerDriver(any(CreateDriverRequest.class)))
                .thenThrow(new IllegalArgumentException("Driver profile already exists for userId: existing-driver"));

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Driver profile already exists for userId: existing-driver"));
    }

    @Test
    @DisplayName("Negative Scenario: Fetching non-existent driver returns 404 Not Found")
    void getDriverById_NotFound_Returns404() throws Exception {
        when(driverService.getDriverById("nonexistent"))
                .thenThrow(new ResourceNotFoundException("Driver not found with id: nonexistent"));

        mockMvc.perform(get("/api/drivers/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Driver not found with id: nonexistent"));
    }

    @Test
    @DisplayName("Success Scenario: Fetching available drivers returns 200 OK")
    void getAvailableDrivers_Returns200WithList() throws Exception {
        DriverResponse driver = DriverResponse.builder()
                .id("d1")
                .userId("u1")
                .fullName("Kamal Perera")
                .status(DriverStatus.AVAILABLE)
                .isAvailable(true)
                .build();

        when(driverService.getAvailableDrivers("Colombo")).thenReturn(List.of(driver));

        mockMvc.perform(get("/api/drivers/available")
                        .param("serviceArea", "Colombo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("d1"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].isAvailable").value(true));
    }
}
