package com.ridelink.fare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload containing the calculated fare estimation and pricing breakdown")
public class FareEstimateResponse {

    @Schema(description = "Evaluated trip distance in kilometers", example = "12.5")
    private Double distanceKm;

    @Schema(description = "Total estimated fare amount", example = "1150.0")
    private Double estimatedFare;

    @Builder.Default
    @Schema(description = "Currency unit of the fare", example = "LKR")
    private String currency = "LKR";

    @Schema(description = "Documented rule applied for this calculation", example = "Base fare: LKR 150.0 + Distance (12.5 km * 80.0/km) = LKR 1150.0 (Min: LKR 250.0)")
    private String pricingRuleDescription;
}
