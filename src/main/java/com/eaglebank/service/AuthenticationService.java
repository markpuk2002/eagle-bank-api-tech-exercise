package com.eaglebank.service;

import com.eaglebank.constants.ApiConstants;
import com.eaglebank.dto.request.AuthenticationRequest;
import com.eaglebank.entity.User;
import com.eaglebank.exception.AuthenticationException;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for user authentication and JWT token management.
 * Handles user authentication and JWT token extraction from headers.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final JwtService jwtService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticates a user with username and password, returning a JWT token if successful.
     * Note: The request is validated at the controller level via {@code @Valid} and {@code @NotBlank} annotations.
     * This method assumes the request has already been validated and contains non-null, non-blank values.
     *
     * @param authenticationRequest The authentication request containing username and password (validated at controller level)
     * @return A JWT token if authentication succeeds
     * @throws AuthenticationException If the username does not exist or the password is incorrect
     */
    public String authenticateUser(AuthenticationRequest authenticationRequest) {
        String username = authenticationRequest.getUsername();
        String password = authenticationRequest.getPassword();
        
        try {
            User user = userService.getUserEntityByUsername(username);
            if (passwordEncoder.matches(password, user.getPassword())) {
                log.info("User {} authenticated successfully", username);
                return jwtService.generateToken(user.getUsername(), user.getId());
            }
            log.warn("Authentication failed for username: {} - invalid password", username);
            throw new AuthenticationException("Invalid username or password");
        } catch (ResourceNotFoundException e) {
            log.warn("Authentication failed for username: {} - user not found", username);
            throw new AuthenticationException("Invalid username or password", e);
        }
    }

    /**
     * Extracts the user ID from an Authorization header containing a JWT Bearer token.
     *
     * @param authHeader The Authorization header value (format: "Bearer {token}")
     * @return The user ID extracted from the JWT token (format: usr-{12 chars})
     * @throws IllegalArgumentException If the Authorization header is missing, invalid, or does not start with "Bearer "
     */
    public String getUserIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(ApiConstants.BEARER_PREFIX)) {
            throw new IllegalArgumentException("Access token is missing or invalid");
        }

        String token = authHeader.substring(ApiConstants.BEARER_PREFIX.length());
        return jwtService.extractUserId(token);
    }
}