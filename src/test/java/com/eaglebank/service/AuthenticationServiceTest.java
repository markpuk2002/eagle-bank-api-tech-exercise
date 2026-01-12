package com.eaglebank.service;

import com.eaglebank.constants.ApiConstants;
import com.eaglebank.dto.request.AuthenticationRequest;
import com.eaglebank.entity.User;
import com.eaglebank.exception.AuthenticationException;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication Service Unit Tests")
class AuthenticationServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    private AuthenticationRequest authenticationRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setUsername("testuser");
        authenticationRequest.setPassword("Password123!");

        testUser = new User();
        testUser.setId("usr-abcdefghijkl");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("$argon2id$encoded-password-hash");
    }

    // --- authenticateUser Tests ---

    @Test
    @DisplayName("authenticateUser - Success with valid credentials")
    void testAuthenticateUser_Success() {
        // Given
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test-token";
        when(userService.getUserEntityByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("Password123!", testUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken("testuser", "usr-abcdefghijkl")).thenReturn(expectedToken);

        // When
        String token = authenticationService.authenticateUser(authenticationRequest);

        // Then
        assertThat(token).isEqualTo(expectedToken);
        verify(userService, times(1)).getUserEntityByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("Password123!", testUser.getPassword());
        verify(jwtService, times(1)).generateToken("testuser", "usr-abcdefghijkl");
    }

    @Test
    @DisplayName("authenticateUser - User not found")
    void testAuthenticateUser_UserNotFound() {
        // Given
        when(userService.getUserEntityByUsername("nonexistent"))
                .thenThrow(new ResourceNotFoundException("User", "nonexistent"));

        authenticationRequest.setUsername("nonexistent");

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticateUser(authenticationRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");

        verify(userService, times(1)).getUserEntityByUsername("nonexistent");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("authenticateUser - Invalid password")
    void testAuthenticateUser_InvalidPassword() {
        // Given
        when(userService.getUserEntityByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("WrongPassword!", testUser.getPassword())).thenReturn(false);

        authenticationRequest.setPassword("WrongPassword!");

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticateUser(authenticationRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");

        verify(userService, times(1)).getUserEntityByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("WrongPassword!", testUser.getPassword());
        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("authenticateUser - Generic exception from userService propagates")
    void testAuthenticateUser_GenericException() {
        // Given
        RuntimeException genericException = new RuntimeException("Database connection failed");
        when(userService.getUserEntityByUsername("testuser")).thenThrow(genericException);

        // When & Then - Generic exceptions should propagate directly, not be wrapped
        assertThatThrownBy(() -> authenticationService.authenticateUser(authenticationRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection failed");

        verify(userService, times(1)).getUserEntityByUsername("testuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("authenticateUser - Verify JWT token generation with correct parameters")
    void testAuthenticateUser_VerifyJwtTokenGeneration() {
        // Given
        String expectedToken = "test-jwt-token";
        when(userService.getUserEntityByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("Password123!", testUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken("testuser", "usr-abcdefghijkl")).thenReturn(expectedToken);

        // When
        authenticationService.authenticateUser(authenticationRequest);

        // Then - Verify JWT service is called with correct username and userId
        verify(jwtService, times(1)).generateToken(eq("testuser"), eq("usr-abcdefghijkl"));
    }

    @Test
    @DisplayName("authenticateUser - Verify password encoder is called with correct password")
    void testAuthenticateUser_VerifyPasswordEncoderCalled() {
        // Given
        when(userService.getUserEntityByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("Password123!", testUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("token");

        // When
        authenticationService.authenticateUser(authenticationRequest);

        // Then - Verify password encoder is called with raw password and encoded password
        verify(passwordEncoder, times(1)).matches(eq("Password123!"), eq(testUser.getPassword()));
    }

    // --- getUserIdFromHeader Tests ---

    @Test
    @DisplayName("getUserIdFromHeader - Success with valid Bearer token")
    void testGetUserIdFromHeader_Success() {
        // Given
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test-token";
        String authHeader = ApiConstants.BEARER_PREFIX + token;
        String expectedUserId = "usr-abcdefghijkl";
        when(jwtService.extractUserId(token)).thenReturn(expectedUserId);

        // When
        String userId = authenticationService.getUserIdFromHeader(authHeader);

        // Then
        assertThat(userId).isEqualTo(expectedUserId);
        verify(jwtService, times(1)).extractUserId(token);
    }

    @Test
    @DisplayName("getUserIdFromHeader - Null header")
    void testGetUserIdFromHeader_NullHeader() {
        // When & Then
        assertThatThrownBy(() -> authenticationService.getUserIdFromHeader(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access token is missing or invalid");

        verify(jwtService, never()).extractUserId(anyString());
    }

    @Test
    @DisplayName("getUserIdFromHeader - Header without Bearer prefix")
    void testGetUserIdFromHeader_WithoutBearerPrefix() {
        // Given
        String invalidHeader = "InvalidHeader token";

        // When & Then
        assertThatThrownBy(() -> authenticationService.getUserIdFromHeader(invalidHeader))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access token is missing or invalid");

        verify(jwtService, never()).extractUserId(anyString());
    }

    @Test
    @DisplayName("getUserIdFromHeader - Empty Bearer token")
    void testGetUserIdFromHeader_EmptyToken() {
        // Given
        String authHeader = ApiConstants.BEARER_PREFIX;
        when(jwtService.extractUserId("")).thenThrow(new IllegalArgumentException("Invalid token"));

        // When & Then
        assertThatThrownBy(() -> authenticationService.getUserIdFromHeader(authHeader))
                .isInstanceOf(IllegalArgumentException.class);

        verify(jwtService, times(1)).extractUserId("");
    }

    @Test
    @DisplayName("getUserIdFromHeader - Verify token extraction removes Bearer prefix correctly")
    void testGetUserIdFromHeader_VerifyTokenExtraction() {
        // Given
        String token = "actual-jwt-token-here";
        String authHeader = ApiConstants.BEARER_PREFIX + token;
        String expectedUserId = "usr-123456789abc";
        when(jwtService.extractUserId(token)).thenReturn(expectedUserId);

        // When
        authenticationService.getUserIdFromHeader(authHeader);

        // Then - Verify JWT service is called with token (without Bearer prefix)
        verify(jwtService, times(1)).extractUserId(eq(token));
        verify(jwtService, never()).extractUserId(eq(authHeader)); // Should not pass full header
    }

    @Test
    @DisplayName("getUserIdFromHeader - Verify substring extraction")
    void testGetUserIdFromHeader_VerifySubstringExtraction() {
        // Given
        String token = "my-jwt-token";
        String authHeader = "Bearer " + token;
        when(jwtService.extractUserId(token)).thenReturn("usr-test");

        // When
        authenticationService.getUserIdFromHeader(authHeader);

        // Then - Verify the token extracted is correct (after "Bearer " prefix)
        verify(jwtService, times(1)).extractUserId(eq(token));
    }

    @Test
    @DisplayName("getUserIdFromHeader - Exception from JWT service")
    void testGetUserIdFromHeader_JwtServiceException() {
        // Given
        String token = "invalid-token";
        String authHeader = ApiConstants.BEARER_PREFIX + token;
        when(jwtService.extractUserId(token))
                .thenThrow(new IllegalArgumentException("Token is expired or invalid"));

        // When & Then
        assertThatThrownBy(() -> authenticationService.getUserIdFromHeader(authHeader))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token is expired or invalid");

        verify(jwtService, times(1)).extractUserId(token);
    }
}
