package com.eaglebank.service;

import com.eaglebank.dto.Address;
import com.eaglebank.dto.request.CreateUserRequest;
import com.eaglebank.dto.request.UpdateUserRequest;
import com.eaglebank.dto.response.UserResponse;
import com.eaglebank.entity.AddressEntity;
import com.eaglebank.entity.User;
import com.eaglebank.exception.ConflictException;
import com.eaglebank.exception.DuplicateResourceException;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.mapper.DtoMapper;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private UserService userService;

    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;
    private User testUser;
    private UserResponse userResponse;
    private AddressEntity addressEntity;
    private String testUserId;
    private String testAuthenticatedUserId;

    @BeforeEach
    void setUp() {
        testUserId = "usr-abcdefghijkl";
        testAuthenticatedUserId = testUserId;

        Address address = new Address();
        address.setLine1("123 Main St");
        address.setTown("London");
        address.setCounty("Greater London");
        address.setPostcode("SW1A 1AA");

        addressEntity = new AddressEntity();
        addressEntity.setLine1("123 Main St");
        addressEntity.setTown("London");
        addressEntity.setCounty("Greater London");
        addressEntity.setPostcode("SW1A 1AA");

        createUserRequest = new CreateUserRequest();
        createUserRequest.setName("John Doe");
        createUserRequest.setAddress(address);
        createUserRequest.setPhoneNumber("+441234567890");
        createUserRequest.setEmail("john.doe@example.com");
        createUserRequest.setUsername("johndoe");
        createUserRequest.setPassword("SecurePass123!");

        updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setName("Jane Doe");
        updateUserRequest.setEmail("jane.doe@example.com");

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername("johndoe");
        testUser.setEmail("john.doe@example.com");
        testUser.setName("John Doe");
        testUser.setPhoneNumber("+441234567890");
        testUser.setAddress(addressEntity);
        testUser.setPassword("$argon2id$encoded-password");
        testUser.setCreatedTimestamp(OffsetDateTime.now());
        testUser.setUpdatedTimestamp(OffsetDateTime.now());

        userResponse = new UserResponse();
        userResponse.setId(testUserId);
        userResponse.setUsername("johndoe");
        userResponse.setEmail("john.doe@example.com");
        userResponse.setName("John Doe");
        userResponse.setPhoneNumber("+441234567890");
        userResponse.setCreatedTimestamp(OffsetDateTime.now());
        userResponse.setUpdatedTimestamp(OffsetDateTime.now());
    }

    @Test
    @DisplayName("createUser - Success")
    void testCreateUser_Success() {
        // Given
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(idGenerator.generateUserId()).thenReturn(testUserId);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$argon2id$encoded");
        when(dtoMapper.toAddressEntity(createUserRequest.getAddress())).thenReturn(addressEntity);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(dtoMapper.toUserResponse(testUser)).thenReturn(userResponse);

        // When
        UserResponse response = userService.createUser(createUserRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testUserId);
        verify(userRepository, times(1)).existsByUsername("johndoe");
        verify(userRepository, times(1)).existsByEmail("john.doe@example.com");
        verify(idGenerator, times(1)).generateUserId();
        verify(passwordEncoder, times(1)).encode("SecurePass123!");
        verify(dtoMapper, times(1)).toAddressEntity(createUserRequest.getAddress());
        verify(userRepository, times(1)).save(any(User.class));
        verify(dtoMapper, times(1)).toUserResponse(testUser);
    }

    @Test
    @DisplayName("createUser - Duplicate username")
    void testCreateUser_DuplicateUsername() {
        // Given
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(createUserRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username")
                .hasMessageContaining("johndoe");

        verify(userRepository, times(1)).existsByUsername("johndoe");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("createUser - Duplicate email")
    void testCreateUser_DuplicateEmail() {
        // Given
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(createUserRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email")
                .hasMessageContaining("john.doe@example.com");

        verify(userRepository, times(1)).existsByUsername("johndoe");
        verify(userRepository, times(1)).existsByEmail("john.doe@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("createUser - Verify user entity is built correctly")
    void testCreateUser_VerifyUserEntityBuiltCorrectly() {
        // Given
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(idGenerator.generateUserId()).thenReturn(testUserId);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$argon2id$encoded");
        when(dtoMapper.toAddressEntity(createUserRequest.getAddress())).thenReturn(addressEntity);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(dtoMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

        // When
        userService.createUser(createUserRequest);

        // Then - Verify user entity is saved with correct values
        verify(userRepository, times(1)).existsByUsername("johndoe");
        verify(userRepository, times(1)).existsByEmail("john.doe@example.com");
        verify(userRepository, times(1)).save(argThat(user ->
                testUserId.equals(user.getId()) &&
                "johndoe".equals(user.getUsername()) &&
                "john.doe@example.com".equals(user.getEmail()) &&
                "John Doe".equals(user.getName()) &&
                "+441234567890".equals(user.getPhoneNumber()) &&
                addressEntity.equals(user.getAddress())
        ));
    }

    @Test
    @DisplayName("getUserById - Success")
    void testGetUserById_Success() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(dtoMapper.toUserResponse(testUser)).thenReturn(userResponse);

        // When
        UserResponse response = userService.getUserById(testUserId, testAuthenticatedUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testUserId);
        verify(userRepository, times(1)).findById(testUserId);
        verify(dtoMapper, times(1)).toUserResponse(testUser);
    }

    @Test
    @DisplayName("getUserById - User not found")
    void testGetUserById_UserNotFound() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(testUserId, testAuthenticatedUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(testUserId);

        verify(userRepository, times(1)).findById(testUserId);
        verify(dtoMapper, never()).toUserResponse(any(User.class));
    }

    @Test
    @DisplayName("getUserByUsername - Success")
    void testGetUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
        when(dtoMapper.toUserResponse(testUser)).thenReturn(userResponse);

        // When
        UserResponse response = userService.getUserByUsername("johndoe");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("johndoe");
        verify(userRepository, times(1)).findByUsername("johndoe");
        verify(dtoMapper, times(1)).toUserResponse(testUser);
    }

    @Test
    @DisplayName("getUserByUsername - User not found")
    void testGetUserByUsername_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserByUsername("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("nonexistent");

        verify(userRepository, times(1)).findByUsername("nonexistent");
        verify(dtoMapper, never()).toUserResponse(any(User.class));
    }

    @Test
    @DisplayName("updateUser - Success with all fields")
    void testUpdateUser_Success() {
        // Given
        User updatedUser = new User();
        updatedUser.setId(testUserId);
        updatedUser.setName("Jane Doe");
        updatedUser.setEmail("jane.doe@example.com");
        updatedUser.setUsername("johndoe");

        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId(testUserId);
        updatedResponse.setName("Jane Doe");
        updatedResponse.setEmail("jane.doe@example.com");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(dtoMapper.toUserResponse(updatedUser)).thenReturn(updatedResponse);

        // When
        UserResponse response = userService.updateUser(testUserId, updateUserRequest, testAuthenticatedUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Jane Doe");
        assertThat(response.getEmail()).isEqualTo("jane.doe@example.com");
        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, times(1)).existsByEmail("jane.doe@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser - User not found")
    void testUpdateUser_UserNotFound() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(testUserId, updateUserRequest, testAuthenticatedUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(testUserId);

        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser - Duplicate email")
    void testUpdateUser_DuplicateEmail() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(testUserId, updateUserRequest, testAuthenticatedUserId))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email")
                .hasMessageContaining("jane.doe@example.com");

        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, times(1)).existsByEmail("jane.doe@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser - Email unchanged (no check needed)")
    void testUpdateUser_EmailUnchanged() {
        // Given
        updateUserRequest.setEmail("john.doe@example.com"); // Same as existing email
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(dtoMapper.toUserResponse(testUser)).thenReturn(userResponse);

        // When
        userService.updateUser(testUserId, updateUserRequest, testAuthenticatedUserId);

        // Then - Should not check for duplicate email when email is unchanged
        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser - Partial update (only name)")
    void testUpdateUser_PartialUpdate() {
        // Given
        UpdateUserRequest partialRequest = new UpdateUserRequest();
        partialRequest.setName("Jane Doe");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(dtoMapper.toUserResponse(testUser)).thenReturn(userResponse);

        // When
        userService.updateUser(testUserId, partialRequest, testAuthenticatedUserId);

        // Then - Verify only name is updated
        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, times(1)).save(argThat(user ->
                "Jane Doe".equals(user.getName()) &&
                "john.doe@example.com".equals(user.getEmail()) // Email unchanged
        ));
    }

    @Test
    @DisplayName("deleteUser - Success")
    void testDeleteUser_Success() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(bankAccountRepository.existsByUser_Id(testUserId)).thenReturn(false);
        doNothing().when(userRepository).deleteById(testUserId);

        // When
        userService.deleteUser(testUserId, testAuthenticatedUserId);

        // Then
        verify(userRepository, times(1)).findById(testUserId);
        verify(bankAccountRepository, times(1)).existsByUser_Id(testUserId);
        verify(userRepository, times(1)).deleteById(testUserId);
    }

    @Test
    @DisplayName("deleteUser - User not found")
    void testDeleteUser_UserNotFound() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(testUserId, testAuthenticatedUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(testUserId);

        verify(userRepository, times(1)).findById(testUserId);
        verify(bankAccountRepository, never()).existsByUser_Id(anyString());
        verify(userRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("deleteUser - Conflict (user has bank accounts)")
    void testDeleteUser_Conflict() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(bankAccountRepository.existsByUser_Id(testUserId)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(testUserId, testAuthenticatedUserId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("A user cannot be deleted when they are associated with a bank account");

        verify(userRepository, times(1)).findById(testUserId);
        verify(bankAccountRepository, times(1)).existsByUser_Id(testUserId);
        verify(userRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("getUserEntity - Success")
    void testGetUserEntity_Success() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        User user = userService.getUserEntity(testUserId);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(testUserId);
        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    @DisplayName("getUserEntity - User not found")
    void testGetUserEntity_UserNotFound() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserEntity(testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(testUserId);

        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    @DisplayName("getUserEntityByUsername - Success")
    void testGetUserEntityByUsername_Success() {
        // Given
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        // When
        User user = userService.getUserEntityByUsername("johndoe");

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("johndoe");
        verify(userRepository, times(1)).findByUsername("johndoe");
    }

    @Test
    @DisplayName("getUserEntityByUsername - User not found")
    void testGetUserEntityByUsername_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserEntityByUsername("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("nonexistent");

        verify(userRepository, times(1)).findByUsername("nonexistent");
    }
}
