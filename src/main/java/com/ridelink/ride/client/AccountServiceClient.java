package com.ridelink.ride.client;

import com.ridelink.ride.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "account-service", url = "${services.account.url}")
public interface AccountServiceClient {

    @GetMapping("/api/users/{id}")
    UserResponse getUserById(@PathVariable("id") String id);
}
