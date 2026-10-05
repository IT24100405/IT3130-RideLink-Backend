package account_service.controller;

import account_service.dto.UpdateProfileRequest;
import account_service.dto.UserResponse;
import account_service.exception.AccountSuspendedException;
import account_service.exception.GlobalExceptionHandler;
import account_service.exception.UserNotFoundException;
import account_service.model.Role;
import account_service.model.UserStatus;
import account_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Negative Scenario: Retrieve user profile when account is SUSPENDED returns 403 Forbidden")
    void getUserById_SuspendedAccount_Returns403Forbidden() throws Exception {
        when(userService.getUserProfile("user-suspended"))
                .thenThrow(new AccountSuspendedException("Account is inactive or suspended"));

        mockMvc.perform(get("/api/users/user-suspended"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Account is inactive or suspended"));
    }

    @Test
    @DisplayName("Negative Scenario: Retrieve user profile when account is DEACTIVATED returns 403 Forbidden")
    void getUserById_DeactivatedAccount_Returns403Forbidden() throws Exception {
        when(userService.getUserProfile("user-deactivated"))
                .thenThrow(new AccountSuspendedException("Account is inactive or suspended"));

        mockMvc.perform(get("/api/users/user-deactivated"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Account is inactive or suspended"));
    }

    @Test
    @DisplayName("Negative Scenario: Retrieve non-existent user returns 404 Not Found")
    void getUserById_NotFound_Returns404NotFound() throws Exception {
        when(userService.getUserProfile("nonexistent"))
                .thenThrow(new UserNotFoundException("User not found with id: nonexistent"));

        mockMvc.perform(get("/api/users/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with id: nonexistent"));
    }

    @Test
    @DisplayName("Success Scenario: Retrieve user by ID returns 200 OK")
    void getUserById_Success_Returns200() throws Exception {
        UserResponse user = UserResponse.builder()
                .id("u1")
                .fullName("John Doe")
                .email("john@ridelink.com")
                .role(Role.PASSENGER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userService.getUserProfile("u1")).thenReturn(user);

        mockMvc.perform(get("/api/users/u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("u1"))
                .andExpect(jsonPath("$.email").value("john@ridelink.com"))
                .andExpect(jsonPath("$.role").value("PASSENGER"));
    }
}
