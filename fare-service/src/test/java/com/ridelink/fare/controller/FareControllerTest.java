package com.ridelink.fare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.fare.dto.FareEstimateRequest;
import com.ridelink.fare.dto.FareEstimateResponse;
import com.ridelink.fare.dto.PaymentReceiptResponse;
import com.ridelink.fare.dto.ProcessPaymentRequest;
import com.ridelink.fare.exception.DuplicatePaymentException;
import com.ridelink.fare.exception.GlobalExceptionHandler;
import com.ridelink.fare.exception.PaymentFailedException;
import com.ridelink.fare.exception.ResourceNotFoundException;
import com.ridelink.fare.model.PaymentMethod;
import com.ridelink.fare.model.PaymentStatus;
import com.ridelink.fare.service.FareService;
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

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FareControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FareService fareService;

    @InjectMocks
    private FareController fareController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fareController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Validation Error: Process payment with missing fields or negative distance returns 400 Bad Request with field errors")
    void processPayment_ValidationError_Returns400() throws Exception {
        ProcessPaymentRequest invalidRequest = ProcessPaymentRequest.builder()
                .rideId("") // blank
                .passengerId("") // blank
                .driverId("") // blank
                .distanceKm(-2.5) // negative
                .paymentMethod(null) // null
                .build();

        mockMvc.perform(post("/api/fares/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.details.rideId").exists())
                .andExpect(jsonPath("$.details.distanceKm").exists())
                .andExpect(jsonPath("$.details.paymentMethod").exists());
    }

    @Test
    @DisplayName("Negative Scenario: Simulated payment gateway failure returns 402 Payment Required")
    void processPayment_SimulateFailure_Returns402PaymentRequired() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .rideId("ride-101")
                .passengerId("p1")
                .driverId("d1")
                .distanceKm(10.0)
                .paymentMethod(PaymentMethod.SIMULATED_CARD)
                .simulateFailure(true)
                .build();

        when(fareService.processPayment(any(ProcessPaymentRequest.class)))
                .thenThrow(new PaymentFailedException("Simulated payment failed: Card declined or insufficient funds"));

        mockMvc.perform(post("/api/fares/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402))
                .andExpect(jsonPath("$.error").value("Payment Required"))
                .andExpect(jsonPath("$.message").value("Simulated payment failed: Card declined or insufficient funds"));
    }

    @Test
    @DisplayName("Negative Scenario: Duplicate payment attempt for same rideId returns 409 Conflict")
    void processPayment_DuplicatePayment_Returns409Conflict() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .rideId("ride-101")
                .passengerId("p1")
                .driverId("d1")
                .distanceKm(10.0)
                .paymentMethod(PaymentMethod.SIMULATED_CARD)
                .build();

        when(fareService.processPayment(any(ProcessPaymentRequest.class)))
                .thenThrow(new DuplicatePaymentException("Payment record already exists for rideId: ride-101"));

        mockMvc.perform(post("/api/fares/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Payment record already exists for rideId: ride-101"));
    }

    @Test
    @DisplayName("Negative Scenario: Fetching receipt for non-existent ride returns 404 Not Found")
    void getReceiptByRideId_NotFound_Returns404() throws Exception {
        when(fareService.getReceiptByRideId("nonexistent"))
                .thenThrow(new ResourceNotFoundException("Payment receipt not found for rideId: nonexistent"));

        mockMvc.perform(get("/api/fares/receipt/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Payment receipt not found for rideId: nonexistent"));
    }

    @Test
    @DisplayName("Success Scenario: Payment processed successfully returns 201 Created with receipt")
    void processPayment_Success_Returns201() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .rideId("ride-101")
                .passengerId("p1")
                .driverId("d1")
                .distanceKm(10.0)
                .paymentMethod(PaymentMethod.SIMULATED_CARD)
                .build();

        PaymentReceiptResponse receipt = PaymentReceiptResponse.builder()
                .receiptNumber("REC-A1B2C3D4E5")
                .rideId("ride-101")
                .passengerId("p1")
                .driverId("d1")
                .totalAmount(950.0)
                .status(PaymentStatus.COMPLETED)
                .paymentMethod(PaymentMethod.SIMULATED_CARD)
                .timestamp(Instant.now())
                .build();

        when(fareService.processPayment(any(ProcessPaymentRequest.class))).thenReturn(receipt);

        mockMvc.perform(post("/api/fares/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNumber").value("REC-A1B2C3D4E5"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalAmount").value(950.0));
    }

    @Test
    @DisplayName("Success Scenario: Fare estimation returns 200 OK")
    void estimateFare_Success_Returns200() throws Exception {
        FareEstimateRequest request = FareEstimateRequest.builder()
                .distanceKm(8.5)
                .pickupLatitude(6.927)
                .pickupLongitude(79.861)
                .destinationLatitude(6.9)
                .destinationLongitude(79.95)
                .build();

        FareEstimateResponse response = FareEstimateResponse.builder()
                .distanceKm(8.5)
                .estimatedFare(830.0)
                .currency("LKR")
                .pricingRuleDescription("max(150 + 8.5*80, 250) = 830 LKR")
                .build();

        when(fareService.estimateFare(any(FareEstimateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedFare").value(830.0))
                .andExpect(jsonPath("$.currency").value("LKR"));
    }
}
