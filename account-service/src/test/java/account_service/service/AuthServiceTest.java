package account_service.service;

import account_service.dto.LoginRequest;
import account_service.dto.LoginResponse;
import account_service.dto.RegisterRequest;
import account_service.dto.UserResponse;
import account_service.exception.AccountSuspendedException;
import account_service.exception.EmailAlreadyExistsException;
import account_service.exception.InvalidCredentialsException;
import account_service.model.Role;
import account_service.model.User;
import account_service.model.UserRole;
import account_service.model.UserStatus;
import account_service.repository.UserRepository;
import account_service.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should successfully login active user and call authenticationManager.authenticate")
    void login_Success() {
        LoginRequest request = new LoginRequest("test@ridelink.com", "password123");
        User user = User.builder()
                .id("u1")
                .email("test@ridelink.com")
                .password("encoded_pass")
                .fullName("Test User")
                .role(Role.PASSENGER)
                .status(UserStatus.ACTIVE)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("test@ridelink.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(user)).thenReturn("mocked_jwt");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mocked_jwt");
        assertThat(response.getUser().getEmail()).isEqualTo("test@ridelink.com");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should reject login when account is SUSPENDED with 403 message")
    void login_SuspendedAccount_ThrowsAccountSuspendedException() {
        LoginRequest request = new LoginRequest("suspended@ridelink.com", "password123");
        User user = User.builder()
                .id("u2")
                .email("suspended@ridelink.com")
                .password("encoded_pass")
                .status(UserStatus.SUSPENDED)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("suspended@ridelink.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountSuspendedException.class)
                .hasMessage("Account is inactive or suspended");
    }

    @Test
    @DisplayName("Should reject login when account is DEACTIVATED with 403 message")
    void login_DeactivatedAccount_ThrowsAccountSuspendedException() {
        LoginRequest request = new LoginRequest("deactivated@ridelink.com", "password123");
        User user = User.builder()
                .id("u3")
                .email("deactivated@ridelink.com")
                .password("encoded_pass")
                .status(UserStatus.DEACTIVATED)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("deactivated@ridelink.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountSuspendedException.class)
                .hasMessage("Account is inactive or suspended");
    }

    @Test
    @DisplayName("Should reject login with invalid password throwing InvalidCredentialsException")
    void login_InvalidPassword_ThrowsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest("test@ridelink.com", "wrongpass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Should handle InternalAuthenticationServiceException throwing InvalidCredentialsException")
    void login_InternalAuthenticationServiceException_ThrowsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest("test@ridelink.com", "anypass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.InternalAuthenticationServiceException("Authentication service error"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Should reject registration when email already exists")
    void register_DuplicateEmail_ThrowsEmailAlreadyExistsException() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("duplicate@ridelink.com")
                .password("Password123")
                .phoneNumber("+1234567890")
                .role(UserRole.PASSENGER)
                .build();

        when(userRepository.existsByEmail("duplicate@ridelink.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Should register new user successfully with encoded password")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("new@ridelink.com")
                .password("Password123")
                .phoneNumber("+1234567890")
                .role(UserRole.DRIVER)
                .build();

        when(userRepository.existsByEmail("new@ridelink.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId("u99");
            return u;
        });

        UserResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("u99");
        assertThat(response.getEmail()).isEqualTo("new@ridelink.com");
        assertThat(response.getRole()).isEqualTo(Role.DRIVER);
        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(any(User.class));
    }
}
