package com.ridelink.fare.controller;

import com.ridelink.fare.dto.FareEstimateRequest;
import com.ridelink.fare.dto.FareEstimateResponse;
import com.ridelink.fare.dto.PaymentReceiptResponse;
import com.ridelink.fare.dto.ProcessPaymentRequest;
import com.ridelink.fare.service.FareService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/fares")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Fare & Payment Service", description = "Endpoints for transparent fare estimation, pricing rule evaluation, payment simulation, and receipt generation")
public class FareController {

    private final FareService fareService;

    @PostMapping("/estimate")
    @Operation(summary = "Calculate fare estimate using documented rule", description = "Calculates estimated fare using transparent pricing formula: max(baseRate + (distanceKm * perKmRate), minimumFare)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fare estimation calculated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for distance parameter")
    })
    public ResponseEntity<FareEstimateResponse> estimateFare(@Valid @RequestBody FareEstimateRequest request) {
        log.info("REST request to calculate fare estimate for distance: {} km", request.getDistanceKm());
        FareEstimateResponse response = fareService.estimateFare(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pay")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record and process a simulated payment", description = "Settles ride fare using simulated cash, card, or wallet. Supports simulateFailure flag for testing negative payment flows.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment successfully processed and receipt issued"),
            @ApiResponse(responseCode = "400", description = "Validation failed, duplicate payment, or simulated payment failure")
    })
    public ResponseEntity<PaymentReceiptResponse> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        log.info("REST request to process payment for rideId: {}, amount for {} km", request.getRideId(), request.getDistanceKm());
        PaymentReceiptResponse receipt = fareService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }

    @GetMapping("/receipt/{rideId}")
    @Operation(summary = "Retrieve receipt by ride ID", description = "Fetches the detailed payment transaction receipt and fare breakdown for a specific ride")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment receipt retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Payment receipt not found for the given ride ID")
    })
    public ResponseEntity<PaymentReceiptResponse> getReceiptByRideId(
            @Parameter(description = "Foreign ride ID reference", required = true)
            @PathVariable("rideId") String rideId) {
        log.info("REST request to retrieve receipt for rideId: {}", rideId);
        PaymentReceiptResponse receipt = fareService.getReceiptByRideId(rideId);
        return ResponseEntity.ok(receipt);
    }
}
