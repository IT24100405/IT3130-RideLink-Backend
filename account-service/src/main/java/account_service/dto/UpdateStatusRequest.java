package account_service.dto;

import account_service.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to update an account status (Admin only)")
public class UpdateStatusRequest {

    @NotNull(message = "Status is required (ACTIVE, SUSPENDED, or DEACTIVATED)")
    @Schema(description = "New account status", example = "SUSPENDED", allowableValues = {"ACTIVE", "SUSPENDED", "DEACTIVATED"})
    private UserStatus status;
}
