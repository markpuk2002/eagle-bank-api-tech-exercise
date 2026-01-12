package com.eaglebank.controller;

import com.eaglebank.dto.request.AuthenticationRequest;
import com.eaglebank.dto.response.AuthenticationResponse;
import com.eaglebank.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 * Handles user login with JWT token generation.
 */
@RestController
@RequestMapping("/v1/auth")
@Validated
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * POST /v1/auth/login
     * Authenticates a user and returns a JWT token if successful.
     *
     * @param authenticationRequest The authentication request containing username and password
     * @return Authentication response with JWT token (HTTP 200 OK)
     * @throws com.eaglebank.exception.AuthenticationException If authentication fails (HTTP 401 Unauthorized)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody AuthenticationRequest authenticationRequest) {
        String token = authenticationService.authenticateUser(authenticationRequest);
        AuthenticationResponse response = new AuthenticationResponse();
        response.setToken(token);
        return ResponseEntity.ok(response);
    }
}
