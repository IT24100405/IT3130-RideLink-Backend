package com.ridelink.ride.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rideServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RideLink - Ride Service API")
                        .description("Microservice managing ride requests, lifecycle state transitions, driver dispatching, and passenger history.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("RideLink Engineering Team")
                                .email("support@ridelink.com")));
    }
}
