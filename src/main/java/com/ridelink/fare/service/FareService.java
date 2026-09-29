package com.ridelink.fare.service;

import com.ridelink.fare.dto.FareEstimateRequest;
import com.ridelink.fare.dto.FareEstimateResponse;
import com.ridelink.fare.dto.PaymentReceiptResponse;
import com.ridelink.fare.dto.ProcessPaymentRequest;
import com.ridelink.fare.exception.DuplicatePaymentException;
import com.ridelink.fare.exception.PaymentFailedException;
import com.ridelink.fare.exception.ResourceNotFoundException;
import com.ridelink.fare.model.Payment;
import com.ridelink.fare.model.PaymentStatus;
import com.ridelink.fare.repository.PaymentRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@Getter
@Setter
public class FareService {

    private final PaymentRepository paymentRepository;

    @Value("${fare.base-rate:150.0}")
    private double baseRate = 150.0;

    @Value("${fare.per-km-rate:80.0}")
    private double perKmRate = 80.0;

    @Value("${fare.minimum-fare:250.0}")
    private double minimumFare = 250.0;

    @org.springframework.beans.factory.annotation.Autowired
    public FareService(
            PaymentRepository paymentRepository,
            @Value("${fare.base-rate:150.0}") double baseRate,
            @Value("${fare.per-km-rate:80.0}") double perKmRate,
            @Value("${fare.minimum-fare:250.0}") double minimumFare) {
        this.paymentRepository = paymentRepository;
        this.baseRate = baseRate;
        this.perKmRate = perKmRate;
        this.minimumFare = minimumFare;
    }

    public FareService(PaymentRepository paymentRepository) {
        this(paymentRepository, 150.0, 80.0, 250.0);
    }


    /**
     * Calculate fare using documented formula:
     * max(baseRate + (distanceKm * perKmRate), minimumFare)
     * Returns FareEstimateResponse documenting the calculation rule.
     */
    public FareEstimateResponse estimateFare(FareEstimateRequest request) {
        if (request == null || request.getDistanceKm() == null) {
            throw new IllegalArgumentException("Distance in kilometers is mandatory for fare estimation");
        }

        double distanceKm = request.getDistanceKm();
        double distanceFare = Math.round(distanceKm * perKmRate * 100.0) / 100.0;
        double calculated = baseRate + distanceFare;
        double estimatedFare = Math.max(calculated, minimumFare);
        estimatedFare = Math.round(estimatedFare * 100.0) / 100.0;

        String ruleDescription = String.format(
                "max(baseRate [%.1f] + (distanceKm [%.2f] * perKmRate [%.1f]), minimumFare [%.1f]) = %.2f LKR",
                baseRate, distanceKm, perKmRate, minimumFare, estimatedFare);

        log.info("Calculated fare estimate for distance {} km: {} LKR (Rule: {})",
                distanceKm, estimatedFare, ruleDescription);

        return FareEstimateResponse.builder()
                .distanceKm(distanceKm)
                .estimatedFare(estimatedFare)
                .currency("LKR")
                .pricingRuleDescription(ruleDescription)
                .build();
    }

    /**
     * Process ride fare payment:
     * 1. Check if a payment record already exists for the given rideId. If so, throw DuplicatePaymentException.
     * 2. Compute the final fare using the documented calculation formula.
     * 3. If simulateFailure == true, mark status as FAILED, record failureReason, save to fare_db, and throw PaymentFailedException.
     * 4. If successful, generate unique receipt number (REC-UUID), mark status as COMPLETED, save to MongoDB, and return receipt.
     */
    public PaymentReceiptResponse processPayment(ProcessPaymentRequest request) {
        log.info("Processing payment for rideId: {}, passengerId: {}, distance: {} km",
                request.getRideId(), request.getPassengerId(), request.getDistanceKm());

        if (paymentRepository.findByRideId(request.getRideId()).isPresent()) {
            log.error("Payment record already exists for rideId: {}", request.getRideId());
            throw new DuplicatePaymentException("Payment record already exists for rideId: " + request.getRideId());
        }

        double distanceKm = request.getDistanceKm();
        double distanceFare = Math.round(distanceKm * perKmRate * 100.0) / 100.0;
        double calculatedTotal = baseRate + distanceFare;
        double totalAmount = Math.max(calculatedTotal, minimumFare);
        totalAmount = Math.round(totalAmount * 100.0) / 100.0;

        String receiptNumber = "REC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        if (Boolean.TRUE.equals(request.getSimulateFailure())) {
            String failureReason = "Simulated payment failed: Card declined or insufficient funds";
            log.warn("Simulated payment failure triggered for rideId: {}", request.getRideId());


            Payment failedPayment = Payment.builder()
                    .rideId(request.getRideId())
                    .passengerId(request.getPassengerId())
                    .driverId(request.getDriverId())
                    .distanceKm(distanceKm)
                    .baseFare(baseRate)
                    .distanceFare(distanceFare)
                    .totalAmount(totalAmount)
                    .paymentMethod(request.getPaymentMethod())
                    .status(PaymentStatus.FAILED)
                    .receiptNumber(receiptNumber)
                    .failureReason(failureReason)
                    .timestamp(Instant.now())
                    .build();

            paymentRepository.save(failedPayment);
            throw new PaymentFailedException(failureReason);
        }

        Payment payment = Payment.builder()
                .rideId(request.getRideId())
                .passengerId(request.getPassengerId())
                .driverId(request.getDriverId())
                .distanceKm(distanceKm)
                .baseFare(baseRate)
                .distanceFare(distanceFare)
                .totalAmount(totalAmount)
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.COMPLETED)
                .receiptNumber(receiptNumber)
                .timestamp(Instant.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment successful for rideId: {} with receipt: {} (Total: {} LKR)",
                request.getRideId(), receiptNumber, totalAmount);

        return PaymentReceiptResponse.fromEntity(savedPayment);
    }

    /**
     * Fetch and return payment receipt by rideId or throw ResourceNotFoundException.
     */
    public PaymentReceiptResponse getReceiptByRideId(String rideId) {
        log.info("Fetching payment receipt for rideId: {}", rideId);

        Payment payment = paymentRepository.findByRideId(rideId)
                .orElseThrow(() -> {
                    log.error("Payment receipt not found for rideId: {}", rideId);
                    return new ResourceNotFoundException("Payment receipt not found for rideId: " + rideId);
                });

        return PaymentReceiptResponse.fromEntity(payment);
    }
}
