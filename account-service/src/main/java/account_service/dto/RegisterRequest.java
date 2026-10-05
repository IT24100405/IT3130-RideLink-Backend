package account_service.dto;

import account_service.model.UserRole;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@Schema(description = "Payload required to register a new user account")
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Schema(description = "Full name of the user", example = "Kamal Perera")
    private String fullName;

    @Schema(description = "First name of the user", example = "Kamal")
    private String firstName;

    @Schema(description = "Last name of the user", example = "Perera")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Schema(description = "Unique email address", example = "kamal.perera@example.lk")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    @Schema(description = "Account password (min 6 characters)", example = "SecurePass123!")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Please provide a valid phone number")
    @Schema(description = "Contact phone number in Sri Lankan format (+94771234567 or 0771234567)", example = "+94771234567")
    private String phoneNumber;

    @Schema(
            description = "Account role for registration",
            example = "PASSENGER",
            allowableValues = {"PASSENGER", "DRIVER"},
            implementation = UserRole.class
    )
    @Builder.Default
    private UserRole role = UserRole.PASSENGER;

    @JsonCreator
    public RegisterRequest(
            @JsonProperty("fullName") String fullName,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("email") String email,
            @JsonProperty("password") String password,
            @JsonProperty("phoneNumber") String phoneNumber,
            @JsonProperty("role") UserRole role) {
        this.firstName = firstName;
        this.lastName = lastName;
        if (fullName != null && !fullName.isBlank()) {
            this.fullName = fullName;
            String[] parts = fullName.trim().split("\\s+", 2);
            if (this.firstName == null) {
                this.firstName = parts[0];
            }
            if (this.lastName == null && parts.length > 1) {
                this.lastName = parts[1];
            }
        } else {
            String f = firstName != null ? firstName.trim() : "";
            String l = lastName != null ? lastName.trim() : "";
            String combined = (f + " " + l).trim();
            this.fullName = !combined.isEmpty() ? combined : null;
        }
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.role = (role != null) ? role : UserRole.PASSENGER;
    }

    @JsonSetter("firstName")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
        computeFullName();
    }

    @JsonSetter("lastName")
    public void setLastName(String lastName) {
        this.lastName = lastName;
        computeFullName();
    }

    @JsonSetter("fullName")
    public void setFullName(String fullName) {
        this.fullName = fullName;
        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+", 2);
            if (this.firstName == null) {
                this.firstName = parts[0];
            }
            if (this.lastName == null && parts.length > 1) {
                this.lastName = parts[1];
            }
        }
    }

    private void computeFullName() {
        String first = this.firstName != null ? this.firstName.trim() : "";
        String last = this.lastName != null ? this.lastName.trim() : "";
        String combined = (first + " " + last).trim();
        if (!combined.isEmpty()) {
            this.fullName = combined;
        }
    }

    public static class RegisterRequestBuilder {
        public RegisterRequestBuilder firstName(String firstName) {
            this.firstName = firstName;
            compute();
            return this;
        }

        public RegisterRequestBuilder lastName(String lastName) {
            this.lastName = lastName;
            compute();
            return this;
        }

        public RegisterRequestBuilder fullName(String fullName) {
            this.fullName = fullName;
            if (fullName != null && !fullName.isBlank()) {
                String[] parts = fullName.trim().split("\\s+", 2);
                if (this.firstName == null) {
                    this.firstName = parts[0];
                }
                if (this.lastName == null && parts.length > 1) {
                    this.lastName = parts[1];
                }
            }
            return this;
        }

        private void compute() {
            if (this.fullName == null || this.fullName.isBlank()) {
                String first = this.firstName != null ? this.firstName.trim() : "";
                String last = this.lastName != null ? this.lastName.trim() : "";
                String combined = (first + " " + last).trim();
                if (!combined.isEmpty()) {
                    this.fullName = combined;
                }
            }
        }
    }
}
