package com.ridelink.ride.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO representing the estimated fare response received from fare-service")
public class FareEstimateResponse {

    @JsonAlias({"fare", "totalFare", "estimatedFare"})
    @Schema(description = "Calculated estimated fare amount", example = "18.50")
    private Double estimatedFare;
}
