package com.ridelink.fare.service;

import com.ridelink.fare.dto.FareEstimateRequest;
import com.ridelink.fare.dto.FareEstimateResponse;
import com.ridelink.fare.dto.PaymentReceiptResponse;
import com.ridelink.fare.dto.ProcessPaymentRequest;
import com.ridelink.fare.exception.DuplicatePaymentException;
import com.ridelink.fare.exception.PaymentFailedException;
import com.ridelink.fare.exception.ResourceNotFoundException;
import com.ridelink.fare.model.Payment;
import com.ridelink.fare.model.PaymentMethod;
import com.ridelink.fare.model.PaymentStatus;
import com.ridelink.fare.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FareServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private FareService fareService;

    private ProcessPaymentRequest validPaymentRequest;
    private Payment sampleCompletedPayment;

    @BeforeEach
    void setUp() {
        fareService = new FareService(paymentRepository, 150.0, 80.0, 250.0);

        validPaymentRequest = ProcessPaymentRequest.builder()
                .rideId("ride-101")
                .passengerId("passenger-201")
                .driverId("driver-301")
                .distanceKm(10.0)
                .paymentMethod(PaymentMethod.SIMULATED_CARD)
                .simulateFailure(false)
                .build();

        sampleCompletedPayment = Payment.builder()
                .id("payment-999")
                .rideId("ride-101")
                .passengerId("passenger-201")
                .driverId("driver-301")
                .distanceKm(10.0)
                .baseFare(150.0)
                .distanceFare(800.0)
                .totalAmount(950.0)
                .paymentMethod(PaymentMethod.SIMULATED_CARD)
                .status(PaymentStatus.COMPLETED)
                .receiptNumber("REC-ABC1234567")
                .timestamp(Instant.now())
                .build();
    }

    // =========================================================================
    // FARE ESTIMATION TESTS: Pricing Formula & Minimum Fare Boundary
    // =========================================================================
    @Test
    @DisplayName("Fare Estimation Test: Accurately calculates fare above minimum fare boundary")
    void estimateFare_Success_StandardDistance() {
        FareEstimateRequest request = FareEstimateRequest.builder()
                .distanceKm(10.0)
                .build();

        FareEstimateResponse response = fareService.estimateFare(request);

        assertNotNull(response);
        assertEquals(10.0, response.getDistanceKm());
        // max(150.0 + (10.0 * 80.0), 250.0) = max(150.0 + 800.0, 250.0) = 950.0
        assertEquals(950.0, response.getEstimatedFare());
        assertEquals("LKR", response.getCurrency());
        assertNotNull(response.getPricingRuleDescription());
        assertTrue(response.getPricingRuleDescription().contains("950.00 LKR"));
    }

    @Test
    @DisplayName("Fare Estimation Test: Respects minimum fare boundary when calculated fare is below minimum")
    void estimateFare_Success_RespectsMinimumFareBoundary() {
        // Distance 0.5 km: base (150.0) + (0.5 * 80.0) = 150 + 40 = 190.0 < 250.0 minimum
        FareEstimateRequest request = FareEstimateRequest.builder()
                .distanceKm(0.5)
                .build();

        FareEstimateResponse response = fareService.estimateFare(request);

        assertNotNull(response);
        assertEquals(0.5, response.getDistanceKm());
        assertEquals(250.0, response.getEstimatedFare(), "Fare should be clamped to minimumFare boundary of 250.0");
        assertEquals("LKR", response.getCurrency());
        assertTrue(response.getPricingRuleDescription().contains("minimumFare [250.0]"));
    }

    @Test
    @DisplayName("Fare Estimation Test: Throws IllegalArgumentException when request or distance is null")
    void estimateFare_ThrowsException_WhenDistanceNull() {
        FareEstimateRequest request = FareEstimateRequest.builder().distanceKm(null).build();

        assertThrows(IllegalArgumentException.class, () -> fareService.estimateFare(request));
        assertThrows(IllegalArgumentException.class, () -> fareService.estimateFare(null));
    }

    // =========================================================================
    // SUCCESSFUL PAYMENT SIMULATION TEST
    // =========================================================================
    @Test
    @DisplayName("Successful Payment Simulation: Generates receipt and saves payment as COMPLETED")
    void processPayment_Success_GeneratesReceiptAndSavesCompleted() {
        when(paymentRepository.findByRideId("ride-101")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId("payment-1001");
            return p;
        });

        PaymentReceiptResponse response = fareService.processPayment(validPaymentRequest);

        assertNotNull(response);
        assertEquals("ride-101", response.getRideId());
        assertEquals("passenger-201", response.getPassengerId());
        assertEquals("driver-301", response.getDriverId());
        assertEquals(10.0, response.getDistanceKm());
        assertEquals(150.0, response.getBaseFare());
        assertEquals(800.0, response.getDistanceFare());
        assertEquals(950.0, response.getTotalAmount());
        assertEquals(PaymentStatus.COMPLETED, response.getStatus());
        assertEquals(PaymentMethod.SIMULATED_CARD, response.getPaymentMethod());
        assertNotNull(response.getReceiptNumber());
        assertTrue(response.getReceiptNumber().startsWith("REC-"));
        assertNotNull(response.getTimestamp());
        assertNull(response.getFailureReason());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertEquals(PaymentStatus.COMPLETED, saved.getStatus());
        assertEquals(950.0, saved.getTotalAmount());
        assertEquals("ride-101", saved.getRideId());
    }

    // =========================================================================
    // NEGATIVE SCENARIO TEST: Simulated Failure
    // =========================================================================
    @Test
    @DisplayName("Negative Scenario Test: When simulateFailure is enabled, PaymentFailedException is raised and status recorded as FAILED")
    void processPayment_NegativeScenario_WhenSimulateFailureIsTrue_ThrowsPaymentFailedException() {
        ProcessPaymentRequest failingRequest = ProcessPaymentRequest.builder()
                .rideId("ride-102")
                .passengerId("passenger-201")
                .driverId("driver-301")
                .distanceKm(5.0)
                .paymentMethod(PaymentMethod.SIMULATED_CARD)
                .simulateFailure(true)
                .build();

        when(paymentRepository.findByRideId("ride-102")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentFailedException exception = assertThrows(PaymentFailedException.class, () ->
                fareService.processPayment(failingRequest)
        );

        assertTrue(exception.getMessage().contains("Simulated payment failed: Card declined or insufficient funds"));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        Payment savedFailedPayment = paymentCaptor.getValue();
        assertEquals(PaymentStatus.FAILED, savedFailedPayment.getStatus());
        assertEquals("ride-102", savedFailedPayment.getRideId());
        assertNotNull(savedFailedPayment.getFailureReason());
        assertTrue(savedFailedPayment.getFailureReason().contains("Simulated payment failed"));
        assertNotNull(savedFailedPayment.getReceiptNumber());
    }

    // =========================================================================
    // DUPLICATE PAYMENT TEST
    // =========================================================================
    @Test
    @DisplayName("Duplicate Payment Test: Processing payment twice for the same rideId throws DuplicatePaymentException")
    void processPayment_ThrowsDuplicatePaymentException_WhenPaymentAlreadyExists() {
        when(paymentRepository.findByRideId("ride-101")).thenReturn(Optional.of(sampleCompletedPayment));

        DuplicatePaymentException exception = assertThrows(DuplicatePaymentException.class, () ->
                fareService.processPayment(validPaymentRequest)
        );

        assertTrue(exception.getMessage().contains("Payment record already exists for rideId: ride-101"));
        verify(paymentRepository, times(1)).findByRideId("ride-101");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    // =========================================================================
    // RECEIPT RETRIEVAL TESTS
    // =========================================================================
    @Test
    @DisplayName("Receipt Retrieval Test: Successfully fetches receipt by ride ID")
    void getReceiptByRideId_Success() {
        when(paymentRepository.findByRideId("ride-101")).thenReturn(Optional.of(sampleCompletedPayment));

        PaymentReceiptResponse response = fareService.getReceiptByRideId("ride-101");

        assertNotNull(response);
        assertEquals("REC-ABC1234567", response.getReceiptNumber());
        assertEquals("ride-101", response.getRideId());
        assertEquals(950.0, response.getTotalAmount());
        assertEquals(PaymentStatus.COMPLETED, response.getStatus());

        verify(paymentRepository, times(1)).findByRideId("ride-101");
    }

    @Test
    @DisplayName("Receipt Retrieval Test: Throws ResourceNotFoundException when payment receipt not found for ride ID")
    void getReceiptByRideId_ThrowsResourceNotFoundException_WhenNotFound() {
        when(paymentRepository.findByRideId("non-existent-ride")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                fareService.getReceiptByRideId("non-existent-ride")
        );

        assertTrue(exception.getMessage().contains("Payment receipt not found for rideId: non-existent-ride"));
        verify(paymentRepository, times(1)).findByRideId("non-existent-ride");
    }
}
