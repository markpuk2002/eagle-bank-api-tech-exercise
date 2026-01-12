package com.eaglebank.service;

import com.eaglebank.dto.request.CreateUserRequest;
import com.eaglebank.dto.request.UpdateUserRequest;
import com.eaglebank.dto.response.UserResponse;
import com.eaglebank.entity.User;
import com.eaglebank.exception.ConflictException;
import com.eaglebank.exception.DuplicateResourceException;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.mapper.DtoMapper;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.util.AuthorizationHelper;
import com.eaglebank.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing users.
 * Handles user creation, creation, retrieval, update, and deletion with proper validation and authorization.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final DtoMapper dtoMapper;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;

    /**
     * Creates a new user with full profile information.
     * Used for admin/user management endpoints.
     * Note: Currently allows any authenticated user to create users. For production, consider adding role-based access control.
     *
     * @param request The user creation request containing all user details (name, address, phone, email, username, password)
     * @return The created user response (password excluded)
     * @throws DuplicateResourceException If the username already exists
     */
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email", request.getEmail());
        }

        String userId = idGenerator.generateUserId();
        
        User user = buildUserEntity(
            userId,
            request.getUsername(),
            request.getEmail(),
            passwordEncoder.encode(request.getPassword()),
            request.getName(),
            dtoMapper.toAddressEntity(request.getAddress()),
            request.getPhoneNumber()
        );

        User savedUser = userRepository.save(user);
        log.info("User created successfully: {} (ID: {})", request.getUsername(), userId);
        return dtoMapper.toUserResponse(savedUser);
    }


    /**
     * Updates an existing user's information (partial update - only non-null fields are updated).
     *
     * @param userId The user ID to update (format: usr-{12 chars})
     * @param request The update request containing new values (only non-null fields will be updated)
     * @param authenticatedUserId The ID of the authenticated user making the request
     * @return The updated user response (password excluded)
     * @throws ResourceNotFoundException If the user with the specified ID does not exist
     * @throws com.eaglebank.exception.UnauthorizedException If the user does not have access to this user
     * @throws DuplicateResourceException If the new email already exists for another user
     */
    public UserResponse updateUser(String userId, UpdateUserRequest request, String authenticatedUserId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        AuthorizationHelper.verifyUserOwnership(user.getId(), authenticatedUserId, "user");

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getAddress() != null) {
            user.setAddress(dtoMapper.toAddressEntity(request.getAddress()));
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Email", request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        User updatedUser = userRepository.save(user);
        return dtoMapper.toUserResponse(updatedUser);
    }

    /**
     * Deletes a user after verifying they have no associated bank accounts.
     *
     * @param userId The user ID to delete (format: usr-{12 chars})
     * @param authenticatedUserId The ID of the authenticated user making the request
     * @throws ResourceNotFoundException If the user with the specified ID does not exist
     * @throws com.eaglebank.exception.UnauthorizedException If the user does not have access to this user
     * @throws ConflictException If the user has associated bank accounts
     */
    public void deleteUser(String userId, String authenticatedUserId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        AuthorizationHelper.verifyUserOwnership(user.getId(), authenticatedUserId, "user");

        // Check if user has associated bank accounts
        if (bankAccountRepository.existsByUser_Id(userId)) {
            throw new ConflictException(
                "A user cannot be deleted when they are associated with a bank account"
            );
        }

        userRepository.deleteById(userId);
        log.info("User deleted successfully: {} (ID: {})", user.getUsername(), userId);
    }

    /**
     * Builds a User entity with the provided information.
     *
     * @param userId The user ID to assign
     * @param username The username
     * @param email The email address
     * @param encodedPassword The encoded password
     * @param name The user's name (optional)
     * @param address The user's address (optional)
     * @param phoneNumber The user's phone number (optional)
     * @return A configured User entity
     */
    private User buildUserEntity(String userId, String username, String email, String encodedPassword,
                                String name, com.eaglebank.entity.AddressEntity address, String phoneNumber) {
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setName(name);
        user.setAddress(address);
        user.setPhoneNumber(phoneNumber);
        return user;
    }

    /**
     * Retrieves a user by their unique ID.
     *
     * @param userId The user ID to retrieve (format: usr-{12 chars})
     * @param authenticatedUserId The ID of the authenticated user making the request
     * @return The user response (password excluded)
     * @throws ResourceNotFoundException If the user with the specified ID does not exist
     * @throws com.eaglebank.exception.UnauthorizedException If the user does not have access to this user
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId, String authenticatedUserId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        AuthorizationHelper.verifyUserOwnership(user.getId(), authenticatedUserId, "user");
        
        return dtoMapper.toUserResponse(user);
    }

    /**
     * Retrieves a user by their username.
     *
     * @param username The username to retrieve
     * @return The user response (password excluded)
     * @throws ResourceNotFoundException If the user with the specified username does not exist
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(dtoMapper::toUserResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }

    /**
     * Retrieves a user entity by ID (internal use only).
     * This method does not perform authorization checks and should be used carefully.
     *
     * @param userId The user ID to retrieve (format: usr-{12 chars})
     * @return The user entity
     * @throws ResourceNotFoundException If the user with the specified ID does not exist
     */
    @Transactional(readOnly = true)
    public User getUserEntity(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    /**
     * Retrieves a user entity by username (internal use only).
     * This method does not perform authorization checks and should be used carefully.
     *
     * @param username The username to retrieve
     * @return The user entity
     * @throws ResourceNotFoundException If the user with the specified username does not exist
     */
    @Transactional(readOnly = true)
    public User getUserEntityByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }
}