package com.eaglebank.controller;

import com.eaglebank.dto.Address;
import com.eaglebank.dto.request.CreateUserRequest;
import com.eaglebank.dto.request.UpdateUserRequest;
import com.eaglebank.dto.response.UserResponse;
import com.eaglebank.exception.ConflictException;
import com.eaglebank.exception.DuplicateResourceException;
import com.eaglebank.exception.ResourceNotFoundException;
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

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Controller Unit Tests")
class UserControllerTest {
    @Mock
    private UserService userService;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private UserController userController;

    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;
    private UserResponse userResponse;
    private String testUserId;
    private String testAuthHeader;
    private String testAuthenticatedUserId;

    @BeforeEach
    void setUp() {
        testUserId = "usr-abcdefghijkl";
        testAuthHeader = "Bearer test-token";
        testAuthenticatedUserId = "usr-testuser123";

        Address address = new Address();
        address.setLine1("123 Main St");
        address.setLine2("Apt 4B");
        address.setLine3(null);
        address.setTown("London");
        address.setCounty("Greater London");
        address.setPostcode("SW1A 1AA");

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

        userResponse = new UserResponse();
        userResponse.setId(testUserId);
        userResponse.setName("John Doe");
        userResponse.setAddress(address);
        userResponse.setPhoneNumber("+441234567890");
        userResponse.setEmail("john.doe@example.com");
        userResponse.setUsername("johndoe");
        userResponse.setCreatedTimestamp(OffsetDateTime.now());
        userResponse.setUpdatedTimestamp(OffsetDateTime.now());
    }

    @Test
    @DisplayName("POST /v1/users - Success")
    void testCreateUser_Success() {
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(userResponse);

        ResponseEntity<UserResponse> response = userController.createUser(createUserRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(testUserId);
        assertThat(response.getBody().getUsername()).isEqualTo("johndoe");

        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
        verify(userService, never()).getUserById(anyString(), anyString());
        verify(userService, never()).updateUser(anyString(), any(UpdateUserRequest.class), anyString());
        verify(userService, never()).deleteUser(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /v1/users - Duplicate username")
    void testCreateUser_DuplicateUsername() {
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username", "johndoe"));

        assertThatThrownBy(() -> userController.createUser(createUserRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username")
                .hasMessageContaining("johndoe");

        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("POST /v1/users - Verify service is called with correct request")
    void testCreateUser_VerifyServiceCalledWithCorrectRequest() {
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(userResponse);

        userController.createUser(createUserRequest);

        verify(userService, times(1)).createUser(argThat(req ->
                req.getUsername().equals("johndoe") &&
                req.getEmail().equals("john.doe@example.com") &&
                req.getName().equals("John Doe") &&
                req.getPhoneNumber().equals("+441234567890") &&
                req.getPassword().equals("SecurePass123!")
        ));
    }

    @Test
    @DisplayName("POST /v1/users - Verify response body is properly constructed")
    void testCreateUser_VerifyResponseBodyConstruction() {
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(userResponse);

        ResponseEntity<UserResponse> response = userController.createUser(createUserRequest);

        assertThat(response.getBody()).isInstanceOf(UserResponse.class);
        assertThat(response.getBody().getId()).isEqualTo(testUserId);
        assertThat(response.getBody().getUsername()).isEqualTo("johndoe");
        assertThat(response.getBody().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("GET /v1/users/{userId} - Success")
    void testFetchUserByID_Success() {
        when(authenticationService.getUserIdFromHeader(anyString())).thenReturn(testAuthenticatedUserId);
        when(userService.getUserById(testUserId, testAuthenticatedUserId)).thenReturn(userResponse);

        ResponseEntity<UserResponse> response = userController.fetchUserByID(testUserId, testAuthHeader);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(testUserId);
        assertThat(response.getBody().getUsername()).isEqualTo("johndoe");

        verify(authenticationService, times(1)).getUserIdFromHeader(testAuthHeader);
        verify(userService, times(1)).getUserById(testUserId, testAuthenticatedUserId);
        verify(userService, never()).createUser(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("GET /v1/users/{userId} - User not found")
    void testFetchUserByID_UserNotFound() {
        when(authenticationService.getUserIdFromHeader(anyString())).thenReturn(testAuthenticatedUserId);
        when(userService.getUserById(testUserId, testAuthenticatedUserId))
                .thenThrow(new ResourceNotFoundException("User", testUserId));

        assertThatThrownBy(() -> userController.fetchUserByID(testUserId, testAuthHeader))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(testUserId);

        verify(authenticationService, times(1)).getUserIdFromHeader(testAuthHeader);
        verify(userService, times(1)).getUserById(testUserId, testAuthenticatedUserId);
    }

    @Test
    @DisplayName("GET /v1/users/{userId} - Verify service is called with correct userId")
    void testFetchUserByID_VerifyServiceCalledWithCorrectUserId() {
        when(authenticationService.getUserIdFromHeader(anyString())).thenReturn(testAuthenticatedUserId);
        when(userService.getUserById(testUserId, testAuthenticatedUserId)).thenReturn(userResponse);

        userController.fetchUserByID(testUserId, testAuthHeader);

        verify(authenticationService, times(1)).getUserIdFromHeader(testAuthHeader);
        verify(userService, times(1)).getUserById(eq(testUserId), eq(testAuthenticatedUserId));
    }

    @Test
    @DisplayName("PATCH /v1/users/{userId} - Success")
    void testUpdateUserByID_Success() {
        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId(testUserId);
        updatedResponse.setName("Jane Doe");
        updatedResponse.setEmail("jane.doe@example.com");
        updatedResponse.setUsername("johndoe");
        updatedResponse.setCreatedTimestamp(OffsetDateTime.now());
        updatedResponse.setUpdatedTimestamp(OffsetDateTime.now());

        when(authenticationService.getUserIdFromHeader(anyString())).thenReturn(testAuthenticatedUserId);
        when(userService.updateUser(eq(testUserId), any(UpdateUserRequest.class), eq(testAuthenticatedUserId)))
                .thenReturn(updatedResponse);

        ResponseEntity<UserResponse> response = userController.updateUserByID(testUserId, testAuthHeader, updateUserRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(testUserId);
        assertThat(response.getBody().getName()).isEqualTo("Jane Doe");
        assertThat(response.getBody().getEmail()).isEqualTo("jane.doe@example.com");

        verify(authenticationService, times(1)).getUserIdFromHeader(testAuthHeader);
        verify(userService, times(1)).updateUser(eq(testUserId), any(UpdateUserRequest.class), eq(testAuthenticatedUserId));
        verify(userService, never()).createUser(any(CreateUserRequest.class));
        verify(userService, never()).deleteUser(anyString(), anyString());
    }

    @Test
    @DisplayName("PATCH /v1/users/{userId} - User not found")
    void testUpdateUserByID_UserNotFound() {
        when(authenticationService.getUserIdFromHeader(anyString())).thenReturn(testAuthenticatedUserId);
        when(userService.updateUser(eq(testUserId), any(UpdateUserRequest.class), eq(testAuthenticatedUserId)))
                .thenThrow(new ResourceNotFoundException("User", testUserId));

        assertThatThrownBy(() -> userController.updateUserByID(testUserId, testAuthHeader, updateUserRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(testUserId);

        verify(authenticationService, times(1)).getUserIdFromHeader(testAuthHeader);
        verify(userService, times(1)).updateUser(eq(testUserId), any(UpdateUserRequest.class), eq(testAuthenticatedUserId));
    }

    @Test
    @DisplayName("PATCH /v1/users/{userId} - Duplicate email")
    void testUpdateUserByID_DuplicateEmail() {
        when(authenticationService.getUserIdFromHeader(anyString())).thenReturn(testAuthenticatedUserId);
        when(userService.updateUser(eq(testUserId), any(UpdateUserRequest.class), eq(testAuthenticatedUserId)))
                .thenThrow(new DuplicateResourceException("Email", "jane.doe@example.com"));

        assertThatThrownBy(() -> userController.updateUserByID(testUserId, testAuthHeader, updateUserRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email")
                .hasMessageContaining("jane.doe@example.com");

        verify(authenticationService, times(1)).getUserIdFromHeader(testAuthHeader);
        verify(userService, times(1)).updateUser(eq(testUserId), any(UpdateUserRequest.class), eq(testAuthenticatedUserId));
    }

    @Test
    @DisplayName("PATCH /v1/users/{userId} - Verify service is called with correct parameters")
    void testUpdateUserByID_VerifyServiceCalledWithCorrectParameters() {
        when(authenticationService.getUserIdFromHeader(anyString())).thenReturn(testAuthenticatedUserId);
        when(userService.updateUser(eq(testUserId), any(UpdateUserRequest.class), eq(testAuthenticatedUserId)))
                .thenReturn(userResponse);

        userController.updateUserByID(testUserId, testAuthHeader, updateUserRequest);

        verify(authenticationService, times(1)).getUserIdFromHeader(testAuthHeader);
        verify(userService, times(1)).updateUser(eq(testUserId), argThat(req ->
                req.getName().equals("Jane Doe") &&
                req.getEmail().equals("jane.doe@example.com")
        ), eq(testAuthenticatedUserId));
    }

    @Test
    @DisplayName("DELETE /v1/users/{userId} - Success")
    void testDeleteUserByID_Success() {
        when(authenticationService.getUserIdFromHeader(anyString())).thenReturn(testAuthenticatedUserId);
        doNothing().when(userService).deleteUser(testUserId, testAuthenticatedUserId);

        ResponseEntity<Void> response = userController.deleteUserByID(testUserId, testAuthHeader);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(authenticationService, times(1)).getUserIdFromHeader(testAuthHeader);
        verify(userService, times(1)).deleteUser(testUserId, testAuthenticatedUserId);
        verify(userService, never()).createUser(any(CreateUserRequest.class));
        verify(userService, never()).getUserById(anyString(), anyString());
        verify(userService, never()).updateUser(anyString(), any(UpdateUserRequest.class), anyString());
    }

    @Test
    @DisplayName("DELETE /v1/users/{userId} - User not found")
    void testDeleteUserByID_UserNotFound() {
        when(authenticationService.getUserIdFromHeader(anyString())).thenReturn(testAuthenticatedUserId);
        doThrow(new ResourceNotFoundException("User", testUserId))
                .when(userService).deleteUser(testUserId, testAuthenticatedUserId);

        assertThatThrownBy(() -> userController.deleteUserByID(testUserId, testAuthHeader))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(testUserId);

        verify(authenticationService, times(1)).getUserIdFromHeader(testAuthHeader);
        verify(userService, times(1)).deleteUser(testUserId, testAuthenticatedUserId);
    }

    @Test
    @DisplayName("DELETE /v1/users/{userId} - Conflict (user has bank accounts)")
    void testDeleteUserByID_Conflict() {
        when(authenticationService.getUserIdFromHeader(anyString())).thenReturn(testAuthenticatedUserId);
        doThrow(new ConflictException("A user cannot be deleted when they are associated with a bank account"))
                .when(userService).deleteUser(testUserId, testAuthenticatedUserId);

        assertThatThrownBy(() -> userController.deleteUserByID(testUserId, testAuthHeader))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("A user cannot be deleted when they are associated with a bank account");

        verify(authenticationService, times(1)).getUserIdFromHeader(testAuthHeader);
        verify(userService, times(1)).deleteUser(testUserId, testAuthenticatedUserId);
    }

    @Test
    @DisplayName("DELETE /v1/users/{userId} - Verify service is called with correct userId")
    void testDeleteUserByID_VerifyServiceCalledWithCorrectUserId() {
        when(authenticationService.getUserIdFromHeader(anyString())).thenReturn(testAuthenticatedUserId);
        doNothing().when(userService).deleteUser(testUserId, testAuthenticatedUserId);

        userController.deleteUserByID(testUserId, testAuthHeader);

        verify(authenticationService, times(1)).getUserIdFromHeader(testAuthHeader);
        verify(userService, times(1)).deleteUser(eq(testUserId), eq(testAuthenticatedUserId));
    }

    @Test
    @DisplayName("DELETE /v1/users/{userId} - Verify response is 204 No Content with null body")
    void testDeleteUserByID_VerifyResponseFormat() {
        when(authenticationService.getUserIdFromHeader(anyString())).thenReturn(testAuthenticatedUserId);
        doNothing().when(userService).deleteUser(testUserId, testAuthenticatedUserId);

        ResponseEntity<Void> response = userController.deleteUserByID(testUserId, testAuthHeader);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }
}
