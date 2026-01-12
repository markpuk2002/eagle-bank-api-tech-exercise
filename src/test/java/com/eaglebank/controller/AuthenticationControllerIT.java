package com.eaglebank.controller;

import com.eaglebank.dto.Address;
import com.eaglebank.dto.request.AuthenticationRequest;
import com.eaglebank.dto.request.CreateUserRequest;
import com.eaglebank.dto.response.AuthenticationResponse;
import com.eaglebank.dto.response.BadRequestErrorResponse;
import com.eaglebank.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@DisplayName("Authentication Controller Integration Tests")
class AuthenticationControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    private static final String CREATE_USER_URL = "/v1/users";
    private static final String LOGIN_URL = "/v1/auth/login";

    @BeforeEach
    void setUp() {
        // Initialize ObjectMapper for JSON serialization/deserialization with Java 8 date/time support
        this.objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        
        // Initialize MockMvc with WebApplicationContext for full integration testing
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        // Each test runs in a transaction that gets rolled back
    }

    @Test
    @DisplayName("POST /v1/auth/login - Success")
    void testLogin_Success() throws Exception {
        // Given - Create a user first
        Address address = new Address();
        address.setLine1("123 Main St");
        address.setLine2("Apt 4B");
        address.setLine3(null);
        address.setTown("London");
        address.setCounty("Greater London");
        address.setPostcode("SW1A 1AA");

        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setName("John Doe");
        createUserRequest.setAddress(address);
        createUserRequest.setPhoneNumber("+441234567890");
        createUserRequest.setUsername("testuser");
        createUserRequest.setEmail("test@example.com");
        createUserRequest.setPassword("Password123!");

        mockMvc.perform(post(CREATE_USER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated());

        // When - Login with correct credentials
        AuthenticationRequest loginRequest = new AuthenticationRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Password123!");

        // Then
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        AuthenticationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthenticationResponse.class);

        assertThat(response.getToken()).isNotNull();
        assertThat(response.getToken()).isNotBlank();
    }

    @Test
    @DisplayName("POST /v1/auth/login - Invalid username")
    void testLogin_InvalidUsername() throws Exception {
        // Given
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("nonexistent");
        request.setPassword("Password123!");

        // When & Then
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).isEqualTo("Invalid username or password");
        assertThat(response.getErrorCode()).isEqualTo("AUTHENTICATION_FAILED");
    }

    @Test
    @DisplayName("POST /v1/auth/login - Invalid password")
    void testLogin_InvalidPassword() throws Exception {
        // Given - Create a user first
        Address address = new Address();
        address.setLine1("123 Main St");
        address.setLine2("Apt 4B");
        address.setLine3(null);
        address.setTown("London");
        address.setCounty("Greater London");
        address.setPostcode("SW1A 1AA");

        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setName("John Doe");
        createUserRequest.setAddress(address);
        createUserRequest.setPhoneNumber("+441234567890");
        createUserRequest.setUsername("testuser");
        createUserRequest.setEmail("test@example.com");
        createUserRequest.setPassword("Password123!");

        mockMvc.perform(post(CREATE_USER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated());

        // When - Login with wrong password
        AuthenticationRequest loginRequest = new AuthenticationRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("WrongPassword123!");

        // Then
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).isEqualTo("Invalid username or password");
        assertThat(response.getErrorCode()).isEqualTo("AUTHENTICATION_FAILED");
    }

    @Test
    @DisplayName("POST /v1/auth/login - Missing username")
    void testLogin_MissingUsername() throws Exception {
        // Given
        AuthenticationRequest request = new AuthenticationRequest();
        request.setPassword("Password123!");

        // When & Then
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        BadRequestErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BadRequestErrorResponse.class);

        assertThat(response.getMessage()).isEqualTo("Validation failed");
        assertThat(response.getDetails().stream()
                .anyMatch(detail -> detail.getField().equals("username")))
                .isTrue();
    }

    @Test
    @DisplayName("POST /v1/auth/login - Missing password")
    void testLogin_MissingPassword() throws Exception {
        // Given
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("testuser");

        // When & Then
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        BadRequestErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BadRequestErrorResponse.class);

        assertThat(response.getMessage()).isEqualTo("Validation failed");
        assertThat(response.getDetails().stream()
                .anyMatch(detail -> detail.getField().equals("password")))
                .isTrue();
    }

    @Test
    @DisplayName("POST /v1/auth/login - Blank username")
    void testLogin_BlankUsername() throws Exception {
        // Given
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("");
        request.setPassword("Password123!");

        // When & Then
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /v1/auth/login - Blank password")
    void testLogin_BlankPassword() throws Exception {
        // Given
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("testuser");
        request.setPassword("");

        // When & Then
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /v1/auth/login - Malformed JSON")
    void testLogin_MalformedJson() throws Exception {
        // When & Then
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }
}
