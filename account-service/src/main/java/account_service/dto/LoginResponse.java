package account_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response containing JWT bearer token and user summary")
public class LoginResponse {

    @Schema(description = "Signed JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Builder.Default
    @Schema(description = "Type of authorization token", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Token validity duration in milliseconds", example = "86400000")
    private long expiresInMs;

    @Schema(description = "User profile summary")
    private UserResponse user;
}
