package account_service.security;

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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Should successfully load user by email and return valid Spring Security User object")
    void loadUserByUsername_Success() {
        User user = User.builder()
                .id("u1")
                .email("driver@ridelink.com")
                .password("$2a$10$hashedpasswordstring")
                .fullName("Driver Dan")
                .role(Role.DRIVER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("driver@ridelink.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("driver@ridelink.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("driver@ridelink.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedpasswordstring");
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_DRIVER");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should normalize email before looking up user")
    void loadUserByUsername_NormalizesEmail() {
        User user = User.builder()
                .id("u2")
                .email("passenger@ridelink.com")
                .password("$2a$10$hashedpassword")
                .role(Role.PASSENGER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("passenger@ridelink.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("  PASSENGER@RideLink.COM  ");

        assertThat(userDetails.getUsername()).isEqualTo("passenger@ridelink.com");
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PASSENGER");
        verify(userRepository).findByEmail("passenger@ridelink.com");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user is not found")
    void loadUserByUsername_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("unknown@ridelink.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown@ridelink.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown@ridelink.com");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when email is null or empty")
    void loadUserByUsername_EmptyEmail_ThrowsException() {
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Email cannot be empty");

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("   "))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Email cannot be empty");
    }
}
