package com.eaglebank.security.service;

import com.eaglebank.entity.User;
import com.eaglebank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Custom User Details Service Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private String testUsername;

    @BeforeEach
    void setUp() {
        testUsername = "testuser";

        testUser = new User();
        testUser.setId("usr-abcdefghijkl");
        testUser.setUsername(testUsername);
        testUser.setEmail("test@example.com");
        testUser.setPassword("$argon2id$encoded-password-hash");
    }

    // --- loadUserByUsername Tests ---

    @Test
    @DisplayName("loadUserByUsername - Success")
    void testLoadUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(testUsername);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(testUsername);
        assertThat(userDetails.getPassword()).isEqualTo(testUser.getPassword());
        verify(userRepository, times(1)).findByUsername(testUsername);
    }

    @Test
    @DisplayName("loadUserByUsername - User not found")
    void testLoadUserByUsername_UserNotFound() {
        // Given
        String nonExistentUsername = "nonexistent";
        when(userRepository.findByUsername(nonExistentUsername)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(nonExistentUsername))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: " + nonExistentUsername);

        verify(userRepository, times(1)).findByUsername(nonExistentUsername);
    }

    @Test
    @DisplayName("loadUserByUsername - Verify UserDetails properties")
    void testLoadUserByUsername_VerifyUserDetailsProperties() {
        // Given
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(testUsername);

        // Then
        assertThat(userDetails.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(userDetails.getPassword()).isEqualTo(testUser.getPassword());
        assertThat(userDetails.getAuthorities()).isNotNull(); // Empty authorities by default
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername - Verify service is called with correct username")
    void testLoadUserByUsername_VerifyServiceCalledWithCorrectUsername() {
        // Given
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));

        // When
        customUserDetailsService.loadUserByUsername(testUsername);

        // Then
        verify(userRepository, times(1)).findByUsername(eq(testUsername));
    }

    @Test
    @DisplayName("loadUserByUsername - Verify with different usernames")
    void testLoadUserByUsername_VerifyWithDifferentUsernames() {
        // Given
        String[] usernames = {"user1", "user2", "admin", "testuser"};

        for (String username : usernames) {
            User user = new User();
            user.setUsername(username);
            user.setPassword("password");

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

            // Then
            assertThat(userDetails.getUsername()).isEqualTo(username);
        }
    }
}
