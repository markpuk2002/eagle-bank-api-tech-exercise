package com.eaglebank.controller;

import com.eaglebank.constants.ApiConstants;
import com.eaglebank.dto.Address;
import com.eaglebank.dto.request.AuthenticationRequest;
import com.eaglebank.dto.request.CreateBankAccountRequest;
import com.eaglebank.dto.request.CreateTransactionRequest;
import com.eaglebank.dto.request.CreateUserRequest;
import com.eaglebank.dto.response.AuthenticationResponse;
import com.eaglebank.dto.response.BadRequestErrorResponse;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.response.ErrorResponse;
import com.eaglebank.dto.response.ListTransactionsResponse;
import com.eaglebank.dto.response.TransactionResponse;
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
@DisplayName("Transaction Controller Integration Tests")
class TransactionControllerIT {

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

    // --- createTransaction Tests ---

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Success (deposit)")
    void testCreateTransaction_SuccessDeposit() throws Exception {
        // Given - Create user, login, and create an account
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(100.0);
        request.setCurrency(BankAccountResponse.Currency.GBP);
        request.setType(CreateTransactionRequest.TransactionType.deposit);
        request.setReference("Test Deposit");

        // When & Then
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        TransactionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TransactionResponse.class);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getId()).matches("^tan-[A-Za-z0-9]{12}$");
        assertThat(response.getAmount()).isEqualTo(100.0);
        assertThat(response.getCurrency()).isEqualTo(BankAccountResponse.Currency.GBP);
        assertThat(response.getType()).isEqualTo(CreateTransactionRequest.TransactionType.deposit);
        assertThat(response.getReference()).isEqualTo("Test Deposit");
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Success (withdrawal)")
    void testCreateTransaction_SuccessWithdrawal() throws Exception {
        // Given - Create user, login, create account, and deposit funds first
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);

        // Deposit funds first
        CreateTransactionRequest depositRequest = new CreateTransactionRequest();
        depositRequest.setAmount(500.0);
        depositRequest.setCurrency(BankAccountResponse.Currency.GBP);
        depositRequest.setType(CreateTransactionRequest.TransactionType.deposit);
        depositRequest.setReference("Initial Deposit");

        mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        // Now withdraw
        CreateTransactionRequest withdrawalRequest = new CreateTransactionRequest();
        withdrawalRequest.setAmount(100.0);
        withdrawalRequest.setCurrency(BankAccountResponse.Currency.GBP);
        withdrawalRequest.setType(CreateTransactionRequest.TransactionType.withdrawal);
        withdrawalRequest.setReference("Test Withdrawal");

        // When & Then
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawalRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        TransactionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TransactionResponse.class);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getAmount()).isEqualTo(100.0);
        assertThat(response.getType()).isEqualTo(CreateTransactionRequest.TransactionType.withdrawal);
        assertThat(response.getReference()).isEqualTo("Test Withdrawal");
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Missing amount")
    void testCreateTransaction_MissingAmount() throws Exception {
        // Given - Create user, login, and create an account
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setCurrency(BankAccountResponse.Currency.GBP);
        request.setType(CreateTransactionRequest.TransactionType.deposit);

        // When & Then
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
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
                .anyMatch(detail -> detail.getField().equals("amount")))
                .isTrue();
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Missing currency")
    void testCreateTransaction_MissingCurrency() throws Exception {
        // Given - Create user, login, and create an account
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(100.0);
        request.setType(CreateTransactionRequest.TransactionType.deposit);

        // When & Then
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
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
                .anyMatch(detail -> detail.getField().equals("currency")))
                .isTrue();
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Missing type")
    void testCreateTransaction_MissingType() throws Exception {
        // Given - Create user, login, and create an account
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(100.0);
        request.setCurrency(BankAccountResponse.Currency.GBP);

        // When & Then
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
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
                .anyMatch(detail -> detail.getField().equals("type")))
                .isTrue();
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Invalid amount (negative)")
    void testCreateTransaction_InvalidAmountNegative() throws Exception {
        // Given - Create user, login, and create an account
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(-10.0); // Negative amount
        request.setCurrency(BankAccountResponse.Currency.GBP);
        request.setType(CreateTransactionRequest.TransactionType.deposit);

        // When & Then
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
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
                .anyMatch(detail -> detail.getField().equals("amount")))
                .isTrue();
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Invalid amount (too large)")
    void testCreateTransaction_InvalidAmountTooLarge() throws Exception {
        // Given - Create user, login, and create an account
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(20000.0); // Exceeds max of 10000.0
        request.setCurrency(BankAccountResponse.Currency.GBP);
        request.setType(CreateTransactionRequest.TransactionType.deposit);

        // When & Then
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
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
                .anyMatch(detail -> detail.getField().equals("amount")))
                .isTrue();
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Missing Authorization header")
    void testCreateTransaction_MissingAuthorization() throws Exception {
        // Given
        String accountNumber = "01123456";
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(100.0);
        request.setCurrency(BankAccountResponse.Currency.GBP);
        request.setType(CreateTransactionRequest.TransactionType.deposit);

        // When & Then - Missing Authorization header returns 403 FORBIDDEN (as per GlobalExceptionHandler)
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
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
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Invalid account number format")
    void testCreateTransaction_InvalidAccountNumberFormat() throws Exception {
        // Given - Create and login a user
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String invalidAccountNumber = "invalid";

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(100.0);
        request.setCurrency(BankAccountResponse.Currency.GBP);
        request.setType(CreateTransactionRequest.TransactionType.deposit);

        // When & Then
        mockMvc.perform(post(ACCOUNTS_URL + "/" + invalidAccountNumber + "/transactions")
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Account not found")
    void testCreateTransaction_AccountNotFound() throws Exception {
        // Given - Create and login a user
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String nonExistentAccountNumber = "01999999"; // Valid format but doesn't exist

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(100.0);
        request.setCurrency(BankAccountResponse.Currency.GBP);
        request.setType(CreateTransactionRequest.TransactionType.deposit);

        // When & Then
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL + "/" + nonExistentAccountNumber + "/transactions")
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("Bank account");
        assertThat(response.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Unauthorized (user does not own account)")
    void testCreateTransaction_Unauthorized() throws Exception {
        // Given - Create two users and an account for the first user
        String firstUserToken = createAndLoginUser("user1", "user1@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(firstUserToken);

        String secondUserToken = createAndLoginUser("user2", "user2@example.com", "Password123!");

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(100.0);
        request.setCurrency(BankAccountResponse.Currency.GBP);
        request.setType(CreateTransactionRequest.TransactionType.deposit);

        // When & Then - Second user tries to create transaction on first user's account
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
                .header("Authorization", ApiConstants.BEARER_PREFIX + secondUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("bank account");
        assertThat(response.getErrorCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Insufficient funds for withdrawal")
    void testCreateTransaction_InsufficientFunds() throws Exception {
        // Given - Create user, login, and create an account (with zero balance)
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(100.0);
        request.setCurrency(BankAccountResponse.Currency.GBP);
        request.setType(CreateTransactionRequest.TransactionType.withdrawal); // Try to withdraw with no balance

        // When & Then
        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("Insufficient funds");
        assertThat(response.getErrorCode()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    // --- listAccountTransaction Tests ---

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions - Success with transactions")
    void testListAccountTransaction_Success() throws Exception {
        // Given - Create user, login, create account, and create a transaction
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);
        String transactionId = createTransactionAndGetTransactionId(authToken, accountNumber);

        // When & Then
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        ListTransactionsResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ListTransactionsResponse.class);

        assertThat(response.getTransactions()).isNotNull();
        assertThat(response.getTransactions()).hasSize(1);
        assertThat(response.getTransactions().get(0).getId()).isEqualTo(transactionId);
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions - Success with empty list")
    void testListAccountTransaction_SuccessEmptyList() throws Exception {
        // Given - Create user, login, and create an account (no transactions)
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);

        // When & Then
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        ListTransactionsResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ListTransactionsResponse.class);

        assertThat(response.getTransactions()).isNotNull();
        assertThat(response.getTransactions()).isEmpty();
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions - Missing Authorization header")
    void testListAccountTransaction_MissingAuthorization() throws Exception {
        // Given
        String accountNumber = "01123456";

        // When & Then - Missing Authorization header returns 403 FORBIDDEN (as per GlobalExceptionHandler)
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + "/transactions"))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("Access token is missing or invalid");
        assertThat(response.getErrorCode()).isEqualTo("ILLEGAL_ARGUMENT");
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions - Invalid account number format")
    void testListAccountTransaction_InvalidAccountNumberFormat() throws Exception {
        // Given - Create and login a user
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String invalidAccountNumber = "invalid";

        // When & Then
        mockMvc.perform(get(ACCOUNTS_URL + "/" + invalidAccountNumber + "/transactions")
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions - Account not found")
    void testListAccountTransaction_AccountNotFound() throws Exception {
        // Given - Create and login a user
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String nonExistentAccountNumber = "01999999"; // Valid format but doesn't exist

        // When & Then
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL + "/" + nonExistentAccountNumber + "/transactions")
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
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions - Unauthorized (user does not own account)")
    void testListAccountTransaction_Unauthorized() throws Exception {
        // Given - Create two users and an account for the first user
        String firstUserToken = createAndLoginUser("user1", "user1@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(firstUserToken);

        String secondUserToken = createAndLoginUser("user2", "user2@example.com", "Password123!");

        // When & Then - Second user tries to list transactions for first user's account
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
                .header("Authorization", ApiConstants.BEARER_PREFIX + secondUserToken))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("bank account");
        assertThat(response.getErrorCode()).isEqualTo("UNAUTHORIZED");
    }

    // --- fetchAccountTransactionByID Tests ---

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Success")
    void testFetchAccountTransactionByID_Success() throws Exception {
        // Given - Create, login, create account, and create a transaction
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);
        String transactionId = createTransactionAndGetTransactionId(authToken, accountNumber);

        // When & Then
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + "/transactions/" + transactionId)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        TransactionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TransactionResponse.class);

        assertThat(response.getId()).isEqualTo(transactionId);
        assertThat(response.getAmount()).isEqualTo(100.0);
        assertThat(response.getType()).isEqualTo(CreateTransactionRequest.TransactionType.deposit);
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Missing Authorization header")
    void testFetchAccountTransactionByID_MissingAuthorization() throws Exception {
        // Given
        String accountNumber = "01123456";
        String transactionId = "tan-1234567890ab";

        // When & Then - Missing Authorization header returns 403 FORBIDDEN (as per GlobalExceptionHandler)
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + "/transactions/" + transactionId))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("Access token is missing or invalid");
        assertThat(response.getErrorCode()).isEqualTo("ILLEGAL_ARGUMENT");
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Invalid account number format")
    void testFetchAccountTransactionByID_InvalidAccountNumberFormat() throws Exception {
        // Given - Create and login a user
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String invalidAccountNumber = "invalid";
        String transactionId = "tan-1234567890ab";

        // When & Then
        mockMvc.perform(get(ACCOUNTS_URL + "/" + invalidAccountNumber + "/transactions/" + transactionId)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Invalid transaction ID format")
    void testFetchAccountTransactionByID_InvalidTransactionIdFormat() throws Exception {
        // Given - Create user, login, and create an account
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);
        String invalidTransactionId = "invalid-id";

        // When & Then
        mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + "/transactions/" + invalidTransactionId)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Account not found")
    void testFetchAccountTransactionByID_AccountNotFound() throws Exception {
        // Given - Create and login a user
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String nonExistentAccountNumber = "01999999"; // Valid format but doesn't exist
        String transactionId = "tan-1234567890ab";

        // When & Then
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL + "/" + nonExistentAccountNumber + "/transactions/" + transactionId)
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
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Transaction not found")
    void testFetchAccountTransactionByID_TransactionNotFound() throws Exception {
        // Given - Create user, login, and create an account
        String authToken = createAndLoginUser("testuser", "test@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(authToken);
        String nonExistentTransactionId = "tan-999999999999"; // Valid format but doesn't exist

        // When & Then
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + "/transactions/" + nonExistentTransactionId)
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("Transaction");
        assertThat(response.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Unauthorized (user does not own account)")
    void testFetchAccountTransactionByID_Unauthorized() throws Exception {
        // Given - Create two users and an account for the first user
        String firstUserToken = createAndLoginUser("user1", "user1@example.com", "Password123!");
        String accountNumber = createAccountAndGetAccountNumber(firstUserToken);
        String transactionId = createTransactionAndGetTransactionId(firstUserToken, accountNumber);

        String secondUserToken = createAndLoginUser("user2", "user2@example.com", "Password123!");

        // When & Then - Second user tries to fetch transaction from first user's account
        MvcResult result = mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + "/transactions/" + transactionId)
                .header("Authorization", ApiConstants.BEARER_PREFIX + secondUserToken))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class);

        assertThat(response.getMessage()).contains("bank account");
        assertThat(response.getErrorCode()).isEqualTo("UNAUTHORIZED");
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

    private String createTransactionAndGetTransactionId(String authToken, String accountNumber) throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(100.0);
        request.setCurrency(BankAccountResponse.Currency.GBP);
        request.setType(CreateTransactionRequest.TransactionType.deposit);
        request.setReference("Test Deposit");

        MvcResult result = mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + "/transactions")
                .header("Authorization", ApiConstants.BEARER_PREFIX + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TransactionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TransactionResponse.class);

        return response.getId();
    }
}
