package com.eaglebank.controller;

import com.eaglebank.dto.request.AuthenticationRequest;
import com.eaglebank.dto.response.AuthenticationResponse;
import com.eaglebank.exception.AuthenticationException;
import com.eaglebank.service.AuthenticationService;
import com.eaglebank.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication Controller Unit Tests")
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private AuthenticationRequest authenticationRequest;

    @BeforeEach
    void setUp() {
        authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setUsername("testuser");
        authenticationRequest.setPassword("Password123!");
    }

    @Test
    @DisplayName("POST /v1/auth/login - Success")
    void testLogin_Success() {
        // Given
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test-token";
        when(authenticationService.authenticateUser(any(AuthenticationRequest.class)))
                .thenReturn(expectedToken);

        // When
        ResponseEntity<AuthenticationResponse> response = 
                authenticationController.login(authenticationRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo(expectedToken);

        // Verify service was called with correct request
        verify(authenticationService, times(1)).authenticateUser(any(AuthenticationRequest.class));
    }

    @Test
    @DisplayName("POST /v1/auth/login - Authentication failed")
    void testLogin_AuthenticationFailed() {
        // Given
        when(authenticationService.authenticateUser(any(AuthenticationRequest.class)))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        // When & Then
        assertThatThrownBy(() -> authenticationController.login(authenticationRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");

        verify(authenticationService, times(1)).authenticateUser(any(AuthenticationRequest.class));
    }

    @Test
    @DisplayName("POST /v1/auth/login - Verify service is called with correct request")
    void testLogin_VerifyServiceCalledWithCorrectRequest() {
        // Given
        String expectedToken = "test-token";
        when(authenticationService.authenticateUser(any(AuthenticationRequest.class)))
                .thenReturn(expectedToken);

        // When
        authenticationController.login(authenticationRequest);

        // Then - Verify service was called with request containing correct username and password
        verify(authenticationService, times(1)).authenticateUser(argThat(req ->
                req.getUsername().equals("testuser") &&
                req.getPassword().equals("Password123!")
        ));
    }

    @Test
    @DisplayName("POST /v1/auth/login - Verify response contains token")
    void testLogin_VerifyResponseContainsToken() {
        // Given
        String expectedToken = "test-jwt-token-12345";
        when(authenticationService.authenticateUser(any(AuthenticationRequest.class)))
                .thenReturn(expectedToken);

        // When
        ResponseEntity<AuthenticationResponse> response = 
                authenticationController.login(authenticationRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo(expectedToken);
        assertThat(response.getBody().getToken()).isNotBlank();
    }

    @Test
    @DisplayName("POST /v1/auth/login - Verify response body is properly constructed")
    void testLogin_VerifyResponseBodyConstruction() {
        // Given
        String expectedToken = "jwt-token-abc123";
        when(authenticationService.authenticateUser(any(AuthenticationRequest.class)))
                .thenReturn(expectedToken);

        // When
        ResponseEntity<AuthenticationResponse> response = 
                authenticationController.login(authenticationRequest);

        // Then
        assertThat(response.getBody()).isInstanceOf(AuthenticationResponse.class);
        assertThat(response.getBody().getToken()).isEqualTo(expectedToken);
    }
}
