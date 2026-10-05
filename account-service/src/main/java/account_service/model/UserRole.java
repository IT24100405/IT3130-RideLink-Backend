package account_service.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User role for registration", enumAsRef = true)
public enum UserRole {
    PASSENGER,
    DRIVER;

    public Role toRole() {
        return Role.valueOf(this.name());
    }

    @JsonCreator
    public static UserRole fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PASSENGER;
        }
        for (UserRole role : UserRole.values()) {
            if (role.name().equalsIgnoreCase(value.trim())) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid user role: " + value + ". Allowed values: PASSENGER, DRIVER");
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
