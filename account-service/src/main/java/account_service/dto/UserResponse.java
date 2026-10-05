package account_service.dto;

import account_service.model.Role;
import account_service.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile representation")
public class UserResponse {

    @Schema(description = "Unique MongoDB ObjectId of the user", example = "64f1a2b3c4d5e6f7a8b9c0d1")
    private String id;

    @Schema(description = "Full name of the user", example = "Kamal Perera")
    private String fullName;

    @Schema(description = "User email address", example = "kamal.perera@example.lk")
    private String email;

    @Schema(description = "User phone number in Sri Lankan format", example = "+94771234567")
    private String phoneNumber;

    @Schema(description = "User system role", example = "PASSENGER", allowableValues = {"PASSENGER", "DRIVER", "ADMIN"})
    private Role role;

    @Schema(description = "Current account status", example = "ACTIVE", allowableValues = {"ACTIVE", "SUSPENDED", "DEACTIVATED"})
    private UserStatus status;

    @Schema(description = "Account creation timestamp", example = "2026-09-27T19:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2026-09-27T20:15:00")
    private LocalDateTime updatedAt;
}
