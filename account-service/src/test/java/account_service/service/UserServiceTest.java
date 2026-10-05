package account_service.service;

import account_service.dto.UpdateProfileRequest;
import account_service.dto.UserResponse;
import account_service.exception.AccountSuspendedException;
import account_service.exception.UserNotFoundException;
import account_service.model.Role;
import account_service.model.User;
import account_service.model.UserStatus;
import account_service.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should retrieve active user profile")
    void getUserProfile_ActiveUser_Success() {
        User user = User.builder()
                .id("u1")
                .email("active@ridelink.com")
                .fullName("Active User")
                .role(Role.PASSENGER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserProfile("u1");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("u1");
        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should throw AccountSuspendedException when user is SUSPENDED")
    void getUserProfile_SuspendedUser_ThrowsAccountSuspendedException() {
        User user = User.builder()
                .id("u2")
                .email("suspended@ridelink.com")
                .fullName("Suspended User")
                .role(Role.DRIVER)
                .status(UserStatus.SUSPENDED)
                .build();

        when(userRepository.findById("u2")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getUserProfile("u2"))
                .isInstanceOf(AccountSuspendedException.class)
                .hasMessage("Account is inactive or suspended");
    }

    @Test
    @DisplayName("Should throw AccountSuspendedException when user is DEACTIVATED")
    void getUserProfile_DeactivatedUser_ThrowsAccountSuspendedException() {
        User user = User.builder()
                .id("u3")
                .email("deactivated@ridelink.com")
                .fullName("Deactivated User")
                .role(Role.PASSENGER)
                .status(UserStatus.DEACTIVATED)
                .build();

        when(userRepository.findById("u3")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getUserProfile("u3"))
                .isInstanceOf(AccountSuspendedException.class)
                .hasMessage("Account is inactive or suspended");
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user does not exist")
    void getUserProfile_NotFound_ThrowsUserNotFoundException() {
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile("nonexistent"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should update user profile when user is active")
    void updateUserProfile_ActiveUser_Success() {
        User user = User.builder()
                .id("u1")
                .email("user@ridelink.com")
                .fullName("Old Name")
                .phoneNumber("1111111")
                .role(Role.PASSENGER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("New Name", "+1234567890");
        UserResponse response = userService.updateUserProfile("u1", request);

        assertThat(response.getFullName()).isEqualTo("New Name");
        assertThat(response.getPhoneNumber()).isEqualTo("+1234567890");
    }

    @Test
    @DisplayName("Should reject profile update when user is SUSPENDED")
    void updateUserProfile_SuspendedUser_ThrowsAccountSuspendedException() {
        User user = User.builder()
                .id("u2")
                .status(UserStatus.SUSPENDED)
                .build();

        when(userRepository.findById("u2")).thenReturn(Optional.of(user));

        UpdateProfileRequest request = new UpdateProfileRequest("New Name", "+1234567890");
        assertThatThrownBy(() -> userService.updateUserProfile("u2", request))
                .isInstanceOf(AccountSuspendedException.class)
                .hasMessage("Account is inactive or suspended");
    }

    @Test
    @DisplayName("Should allow admin to update user status")
    void updateUserStatus_Success() {
        User user = User.builder()
                .id("u1")
                .status(UserStatus.SUSPENDED)
                .build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUserStatus("u1", UserStatus.ACTIVE);

        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
