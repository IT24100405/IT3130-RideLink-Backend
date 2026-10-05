package com.ridelink.driver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "User profile representation received from account-service")
public class UserResponse {

    @Schema(description = "Unique identifier of the user", example = "64f1a2b3c4d5e6f7a8b9c0d1")
    private String id;

    @Schema(description = "User email address", example = "driver@example.com")
    private String email;

    @Schema(description = "User role in the system", example = "DRIVER")
    private String role;

    @JsonProperty("data")
    private void unpackData(Map<String, Object> data) {
        if (data != null) {
            if (data.containsKey("id") && data.get("id") != null) {
                this.id = data.get("id").toString();
            }
            if (data.containsKey("email") && data.get("email") != null) {
                this.email = data.get("email").toString();
            }
            if (data.containsKey("role") && data.get("role") != null) {
                this.role = data.get("role").toString();
            }
        }
    }
}
