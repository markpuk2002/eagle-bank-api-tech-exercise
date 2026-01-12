package com.eaglebank.controller;

import com.eaglebank.constants.ApiConstants;
import com.eaglebank.dto.Address;
import com.eaglebank.dto.request.AuthenticationRequest;
import com.eaglebank.dto.request.CreateBankAccountRequest;
import com.eaglebank.dto.request.CreateUserRequest;
import com.eaglebank.dto.request.UpdateBankAccountRequest;
import com.eaglebank.dto.response.AuthenticationResponse;
import com.eaglebank.dto.response.BadRequestErrorResponse;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.response.ErrorResponse;
import com.eaglebank.dto.response.ListBankAccountsResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@DisplayName("Account Controller Integration Tests")
class AccountControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    private static final String ACCOUNTS_URL = "/v1/accounts";
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
    }

    // --- createAccount Tests ---

    @Test
    @DisplayName("POST /v1/accounts - Success")
    void testCreateAccount_Success() throws Exception {
        // Given - Create and login a user first
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");

        CreateBankAccountRequest request = new CreateBankAccountRequest();
        request.setName("My Savings Account");
        request.setAccountType(CreateBankAccountRequest.AccountType.personal);

        // When & Then
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        BankAccountResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BankAccountResponse.class);

        assertThat(response.getAccountNumber()).isNotNull();
        assertThat(response.getAccountNumber()).matches("^01\\d{6}$");
        assertThat(response.getName()).isEqualTo("My Savings Account");
        assertThat(response.getAccountType()).isEqualTo(CreateBankAccountRequest.AccountType.personal);
        assertThat(response.getBalance()).isEqualTo(0.0);
        assertThat(response.getCurrency()).isEqualTo(BankAccountResponse.Currency.GBP);
    }

    @Test
    @DisplayName("POST /v1/accounts - Missing name")
    void testCreateAccount_MissingName() throws Exception {
        // Given - Create and login a user first
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");

        CreateBankAccountRequest request = new CreateBankAccountRequest();
        request.setAccountType(CreateBankAccountRequest.AccountType.personal);

        // When & Then
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        BadRequestErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BadRequestErrorResponse.class);

        assertThat(response.getMessage()).isEqualTo("Validation failed");
        assertThat(response.getDetails().stream()
                .anyMatch(detail -> detail.getField().equals("name")))
                .isTrue();
    }

    @Test
    @DisplayName("POST /v1/accounts - Missing account type")
    void testCreateAccount_MissingAccountType() throws Exception {
        // Given - Create and login a user first
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");

        CreateBankAccountRequest request = new CreateBankAccountRequest();
        request.setName("My Savings Account");

        // When & Then
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        BadRequestErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BadRequestErrorResponse.class);

        assertThat(response.getMessage()).isEqualTo("Validation failed");
        assertThat(response.getDetails().stream()
                .anyMatch(detail -> detail.getField().equals("accountType")))
                .isTrue();
    }

    @Test
    @DisplayName("POST /v1/accounts - Missing Authorization header")
    void testCreateAccount_MissingAuthorization() throws Exception {
        // Given
        CreateBankAccountRequest request = new CreateBankAccountRequest();
        request.setName("My Savings Account");
        request.setAccountType(CreateBankAccountRequest.AccountType.personal);

        // When & Then - Missing Authorization header returns 403 FORBIDDEN (as per GlobalExceptionHandler)
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("Access token is missing or invalid");
        assertThat(response.getErrorCode()).isEqualTo("ILLEGAL_ARGUMENT");
    }

    @Test
    @DisplayName("POST /v1/accounts - Invalid Authorization header")
    void testCreateAccount_InvalidAuthorization() throws Exception {
        // Given
        CreateBankAccountRequest request = new CreateBankAccountRequest();
        request.setName("My Savings Account");
        request.setAccountType(CreateBankAccountRequest.AccountType.personal);

        // When & Then - Invalid token format returns 403 FORBIDDEN (as per GlobalExceptionHandler)
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL)
                .header("Authorization", "InvalidToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("Access token is missing or invalid");
        assertThat(response.getErrorCode()).isEqualTo("ILLEGAL_ARGUMENT");
    }

    // --- listAccounts Tests ---

    @Test
    @DisplayName("GET /v1/accounts - Success with accounts")
    void testListAccounts_Success() throws Exception {
        // Given - Create user, login, and create an account
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);

        // When & Then
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        ListBankAccountsResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ListBankAccountsResponse.class);

        assertThat(response.getAccounts()).isNotNull();
        assertThat(response.getAccounts()).hasSize(1);
        assertThat(response.getAccounts().get(0).getAccountNumber()).isEqualTo(accountNumber);
    }

    @Test
    @DisplayName("GET /v1/accounts - Success with empty list")
    void testListAccounts_SuccessEmptyList() throws Exception {
        // Given - Create and login a user (no accounts created)
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");

        // When & Then
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        ListBankAccountsResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ListBankAccountsResponse.class);

        assertThat(response.getAccounts()).isNotNull();
        assertThat(response.getAccounts()).isEmpty();
    }

    @Test
    @DisplayName("GET /v1/accounts - Missing Authorization header")
    void testListAccounts_MissingAuthorization() throws Exception {
        // When & Then - Missing Authorization header returns 403 FORBIDDEN (as per GlobalExceptionHandler)
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("Access token is missing or invalid");
        assertThat(response.getErrorCode()).isEqualTo("ILLEGAL_ARGUMENT");
    }

    // --- fetchAccountByAccountNumber Tests ---

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber} - Success")
    void testFetchAccountByAccountNumber_Success() throws Exception {
        // Given - Create user, login, and create an account
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);

        // When & Then
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        BankAccountResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BankAccountResponse.class);

        assertThat(response.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(response.getName()).isEqualTo("My Savings Account");
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber} - Account not found")
    void testFetchAccountByAccountNumber_AccountNotFound() throws Exception {
        // Given - Create and login a user
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String nonExistentAccountNumber = "01999999"; // Valid format but doesn't exist

        // When & Then
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL + "/" + nonExistentAccountNumber)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("Bank account");
        assertThat(response.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber} - Unauthorized (user does not own account)")
    void testFetchAccountByAccountNumber_Unauthorized() throws Exception {
        // Given - Create two users and an account for the first user
        String firstUserToken = createAndLoginUser("user1", "user1@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(firstUserToken);

        String secondUserToken = createAndLoginUser("user2", "user2@example.com", "Password123!");

        // When & Then - Second user tries to access first user's account
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber)
                .header("Authorization", ApiConstants.BEARER_PREFIX + secondUserToken))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("bank account");
        assertThat(response.getErrorCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber} - Invalid account number format")
    void testFetchAccountByAccountNumber_InvalidFormat() throws Exception {
        // Given - Create and login a user
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String invalidAccountNumber = "invalid";

        // When & Then
        mockMvc.perform(get(ACCOUNTS_URL + "/" + invalidAccountNumber)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber} - Missing Authorization header")
    void testFetchAccountByAccountNumber_MissingAuthorization() throws Exception {
        // Given
        String accountNumber = "01123456";

        // When & Then - Missing Authorization header returns 403 FORBIDDEN (as per GlobalExceptionHandler)
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("Access token is missing or invalid");
        assertThat(response.getErrorCode()).isEqualTo("ILLEGAL_ARGUMENT");
    }

    // --- updateAccountByAccountNumber Tests ---

    @Test
    @DisplayName("PATCH /v1/accounts/{accountNumber} - Success")
    void testUpdateAccountByAccountNumber_Success() throws Exception {
        // Given - Create user, login, and create an account
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);

        UpdateBankAccountRequest updateRequest = new UpdateBankAccountRequest();
        updateRequest.setName("Updated Account Name");
        updateRequest.setAccountType(CreateBankAccountRequest.AccountType.personal);

        // When & Then
        MvcResult result = mockMvc.perform(patch(ACCOUNTS_URL + "/" + accountNumber)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        BankAccountResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BankAccountResponse.class);

        assertThat(response.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(response.getName()).isEqualTo("Updated Account Name");
    }

    @Test
    @DisplayName("PATCH /v1/accounts/{accountNumber} - Partial update (only name)")
    void testUpdateAccountByAccountNumber_PartialUpdate() throws Exception {
        // Given - Create user, login, and create an account
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);

        UpdateBankAccountRequest updateRequest = new UpdateBankAccountRequest();
        updateRequest.setName("Updated Account Name");

        // When & Then
        MvcResult result = mockMvc.perform(patch(ACCOUNTS_URL + "/" + accountNumber)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andReturn();

        BankAccountResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BankAccountResponse.class);

        assertThat(response.getName()).isEqualTo("Updated Account Name");
        // Account type should remain unchanged
        assertThat(response.getAccountType()).isEqualTo(CreateBankAccountRequest.AccountType.personal);
    }

    @Test
    @DisplayName("PATCH /v1/accounts/{accountNumber} - Account not found")
    void testUpdateAccountByAccountNumber_AccountNotFound() throws Exception {
        // Given - Create and login a user
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String nonExistentAccountNumber = "01999999"; // Valid format but doesn't exist

        UpdateBankAccountRequest updateRequest = new UpdateBankAccountRequest();
        updateRequest.setName("Updated Account Name");

        // When & Then
        MvcResult result = mockMvc.perform(patch(ACCOUNTS_URL + "/" + nonExistentAccountNumber)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("Bank account");
        assertThat(response.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("PATCH /v1/accounts/{accountNumber} - Unauthorized (user does not own account)")
    void testUpdateAccountByAccountNumber_Unauthorized() throws Exception {
        // Given - Create two users and an account for the first user
        String firstUserToken = createAndLoginUser("user1", "user1@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(firstUserToken);

        String secondUserToken = createAndLoginUser("user2", "user2@example.com", "Password123!");

        UpdateBankAccountRequest updateRequest = new UpdateBankAccountRequest();
        updateRequest.setName("Updated Account Name");

        // When & Then - Second user tries to update first user's account
        MvcResult result = mockMvc.perform(patch(ACCOUNTS_URL + "/" + accountNumber)
                .header("Authorization", ApiConstants.BEARER_PREFIX + secondUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("bank account");
        assertThat(response.getErrorCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("PATCH /v1/accounts/{accountNumber} - Invalid account number format")
    void testUpdateAccountByAccountNumber_InvalidFormat() throws Exception {
        // Given - Create and login a user
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String invalidAccountNumber = "invalid";

        UpdateBankAccountRequest updateRequest = new UpdateBankAccountRequest();
        updateRequest.setName("Updated Account Name");

        // When & Then
        mockMvc.perform(patch(ACCOUNTS_URL + "/" + invalidAccountNumber)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /v1/accounts/{accountNumber} - Missing Authorization header")
    void testUpdateAccountByAccountNumber_MissingAuthorization() throws Exception {
        // Given
        String accountNumber = "01123456";
        UpdateBankAccountRequest updateRequest = new UpdateBankAccountRequest();
        updateRequest.setName("Updated Account Name");

        // When & Then - Missing Authorization header returns 403 FORBIDDEN (as per GlobalExceptionHandler)
        MvcResult result = mockMvc.perform(patch(ACCOUNTS_URL + "/" + accountNumber)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("Access token is missing or invalid");
        assertThat(response.getErrorCode()).isEqualTo("ILLEGAL_ARGUMENT");
    }

    // --- deleteAccountByAccountNumber Tests ---

    @Test
    @DisplayName("DELETE /v1/accounts/{accountNumber} - Success")
    void testDeleteAccountByAccountNumber_Success() throws Exception {
        // Given - Create user, login, and create an account
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);

        // When & Then
        mockMvc.perform(delete(ACCOUNTS_URL + "/" + accountNumber)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isNoContent());

        // Verify account is deleted
        mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /v1/accounts/{accountNumber} - Account not found")
    void testDeleteAccountByAccountNumber_AccountNotFound() throws Exception {
        // Given - Create and login a user
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String nonExistentAccountNumber = "01999999"; // Valid format but doesn't exist

        // When & Then
        MvcResult result = mockMvc.perform(delete(ACCOUNTS_URL + "/" + nonExistentAccountNumber)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("Bank account");
        assertThat(response.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("DELETE /v1/accounts/{accountNumber} - Unauthorized (user does not own account)")
    void testDeleteAccountByAccountNumber_Unauthorized() throws Exception {
        // Given - Create two users and an account for the first user
        String firstUserToken = createAndLoginUser("user1", "user1@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(firstUserToken);

        String secondUserToken = createAndLoginUser("user2", "user2@example.com", "Password123!");

        // When & Then - Second user tries to delete first user's account
        MvcResult result = mockMvc.perform(delete(ACCOUNTS_URL + "/" + accountNumber)
                .header("Authorization", ApiConstants.BEARER_PREFIX + secondUserToken))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("bank account");
        assertThat(response.getErrorCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("DELETE /v1/accounts/{accountNumber} - Invalid account number format")
    void testDeleteAccountByAccountNumber_InvalidFormat() throws Exception {
        // Given - Create and login a user
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String invalidAccountNumber = "invalid";

        // When & Then
        mockMvc.perform(delete(ACCOUNTS_URL + "/" + invalidAccountNumber)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /v1/accounts/{accountNumber} - Missing Authorization header")
    void testDeleteAccountByAccountNumber_MissingAuthorization() throws Exception {
        // Given
        String accountNumber = "01123456";

        // When & Then - Missing Authorization header returns 403 FORBIDDEN (as per GlobalExceptionHandler)
        MvcResult result = mockMvc.perform(delete(ACCOUNTS_URL + "/" + accountNumber))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("Access token is missing or invalid");
        assertThat(response.getErrorCode()).isEqualTo("ILLEGAL_ARGUMENT");
    }

    // --- Helper Methods ---

    private String createAndLoginUser(String username, String email, String password) throws Exception {
        // Create user
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
        createUserRequest.setUsername(username);
        createUserRequest.setEmail(email);
        createUserRequest.setPassword(password);

        mockMvc.perform(post(CREATE_USER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated());

        // Login user
        AuthenticationRequest loginRequest = new AuthenticationRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthenticationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthenticationResponse.class);

        return response.getToken();
    }

    private String createAccountAndGetAccountNumber(String authToken) throws Exception {
        CreateBankAccountRequest request = new CreateBankAccountRequest();
        request.setName("My Savings Account");
        request.setAccountType(CreateBankAccountRequest.AccountType.personal);

        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        BankAccountResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BankAccountResponse.class);

        return response.getAccountNumber();
    }
}
