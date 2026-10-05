package com.ridelink.ride.client;

import com.ridelink.ride.dto.FareEstimateRequest;
import com.ridelink.ride.dto.FareEstimateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "fare-service", url = "${services.fare.url}")
public interface FareServiceClient {

    @PostMapping("/api/fares/estimate")
    FareEstimateResponse estimateFare(@RequestBody FareEstimateRequest request);
}
