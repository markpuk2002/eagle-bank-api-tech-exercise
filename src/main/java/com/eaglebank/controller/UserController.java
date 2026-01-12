package com.eaglebank.controller;

import com.eaglebank.dto.request.CreateUserRequest;
import com.eaglebank.dto.request.UpdateUserRequest;
import com.eaglebank.dto.response.UserResponse;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.service.AuthenticationService;
import com.eaglebank.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for user management endpoints.
 * Provides CRUD operations for users including creation, retrieval, update, and deletion.
 */
@RestController
@RequestMapping("/v1/users")
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    /**
     * POST /v1/users
     * Creates a new user with full profile information.
     *
     * @param request The user creation request containing all user details
     * @return The created user response (HTTP 201 Created)
     * @throws IllegalArgumentException If validation fails or username already exists
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /v1/users/{userId}
     * Retrieves a user by their unique ID.
     *
     * @param userId The user ID to retrieve (format: usr-{12 chars})
     * @param authHeader The Authorization header containing JWT Bearer token
     * @return The user response (HTTP 200 OK)
     * @throws ResourceNotFoundException If the user does not exist
     * @throws com.eaglebank.exception.UnauthorizedException If the user does not have access to this user
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> fetchUserByID(
            @PathVariable
            @Pattern(regexp = "^usr-[A-Za-z0-9]{12}$", message = "User ID must match pattern ^usr-[A-Za-z0-9]{12}$ (12 alphanumeric characters)")
            String userId,
            @RequestHeader(value = "Authorization", required = true) String authHeader) {
        String authenticatedUserId = authenticationService.getUserIdFromHeader(authHeader);
        UserResponse response = userService.getUserById(userId, authenticatedUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /v1/users/{userId}
     * Updates an existing user's information (partial update).
     *
     * @param userId The user ID to update (format: usr-{12 chars})
     * @param authHeader The Authorization header containing JWT Bearer token
     * @param request The update request containing new values (only non-null fields will be updated)
     * @return The updated user response (HTTP 200 OK)
     * @throws ResourceNotFoundException If the user does not exist
     * @throws com.eaglebank.exception.UnauthorizedException If the user does not have access to this user
     * @throws IllegalArgumentException If validation fails or email already exists
     */
    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUserByID(
            @PathVariable
            @Pattern(regexp = "^usr-[A-Za-z0-9]{12}$", message = "User ID must match pattern ^usr-[A-Za-z0-9]{12}$ (12 alphanumeric characters)")
            String userId,
            @RequestHeader(value = "Authorization", required = true) String authHeader,
            @Valid @RequestBody UpdateUserRequest request) {
        String authenticatedUserId = authenticationService.getUserIdFromHeader(authHeader);
        UserResponse response = userService.updateUser(userId, request, authenticatedUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /v1/users/{userId}
     * Deletes a user after verifying they have no associated bank accounts.
     *
     * @param userId The user ID to delete (format: usr-{12 chars})
     * @param authHeader The Authorization header containing JWT Bearer token
     * @return Empty response (HTTP 204 No Content)
     * @throws ResourceNotFoundException If the user does not exist
     * @throws com.eaglebank.exception.UnauthorizedException If the user does not have access to this user
     * @throws ResponseStatusException If the user has associated bank accounts (HTTP 409 Conflict)
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUserByID(
            @PathVariable
            @Pattern(regexp = "^usr-[A-Za-z0-9]{12}$", message = "User ID must match pattern ^usr-[A-Za-z0-9]{12}$ (12 alphanumeric characters)")
            String userId,
            @RequestHeader(value = "Authorization", required = true) String authHeader) {
        String authenticatedUserId = authenticationService.getUserIdFromHeader(authHeader);
        userService.deleteUser(userId, authenticatedUserId);
        return ResponseEntity.noContent().build();
    }
}