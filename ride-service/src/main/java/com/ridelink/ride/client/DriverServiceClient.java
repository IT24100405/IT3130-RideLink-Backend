package com.ridelink.ride.client;

import com.ridelink.ride.dto.DriverDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "driver-service", url = "${services.driver.url}")
public interface DriverServiceClient {

    @GetMapping("/api/drivers/available")
    List<DriverDto> getAvailableDrivers(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng
    );
}
