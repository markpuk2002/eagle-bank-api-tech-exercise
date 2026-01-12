package com.eaglebank.controller;

import com.eaglebank.constants.ApiConstants;
import com.eaglebank.dto.request.CreateTransactionRequest;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.response.ListTransactionsResponse;
import com.eaglebank.dto.response.TransactionResponse;
import com.eaglebank.exception.InsufficientFundsException;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.exception.UnauthorizedException;
import com.eaglebank.service.AuthenticationService;
import com.eaglebank.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Controller Unit Tests")
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private TransactionController transactionController;

    private CreateTransactionRequest createTransactionRequest;
    private TransactionResponse transactionResponse;
    private ListTransactionsResponse listTransactionsResponse;
    private String testUserId;
    private String testAccountNumber;
    private String testTransactionId;
    private String authHeader;

    @BeforeEach
    void setUp() {
        testUserId = "usr-abcdefghijkl";
        testAccountNumber = "01123456";
        testTransactionId = "tan-abcdefghijkl";
        authHeader = ApiConstants.BEARER_PREFIX + "test-jwt-token";

        createTransactionRequest = new CreateTransactionRequest();
        createTransactionRequest.setAmount(100.0);
        createTransactionRequest.setCurrency(BankAccountResponse.Currency.GBP);
        createTransactionRequest.setType(CreateTransactionRequest.TransactionType.deposit);
        createTransactionRequest.setReference("Deposit from ATM");

        transactionResponse = new TransactionResponse();
        transactionResponse.setId(testTransactionId);
        transactionResponse.setAmount(100.0);
        transactionResponse.setCurrency(BankAccountResponse.Currency.GBP);
        transactionResponse.setType(CreateTransactionRequest.TransactionType.deposit);
        transactionResponse.setReference("Deposit from ATM");
        transactionResponse.setUserId(testUserId);
        transactionResponse.setCreatedTimestamp(OffsetDateTime.now());

        listTransactionsResponse = new ListTransactionsResponse();
        listTransactionsResponse.setTransactions(List.of(transactionResponse));
    }

    // --- createTransaction Tests ---

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Success")
    void testCreateTransaction_Success() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.createTransaction(eq(testAccountNumber), any(CreateTransactionRequest.class), eq(testUserId)))
                .thenReturn(transactionResponse);

        // When
        ResponseEntity<TransactionResponse> response = 
                transactionController.createTransaction(testAccountNumber, authHeader, createTransactionRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(testTransactionId);
        assertThat(response.getBody().getAmount()).isEqualTo(100.0);

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(transactionService, times(1)).createTransaction(eq(testAccountNumber), any(CreateTransactionRequest.class), eq(testUserId));
        verify(transactionService, never()).listTransactions(anyString(), anyString());
        verify(transactionService, never()).getTransactionById(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Account not found")
    void testCreateTransaction_AccountNotFound() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.createTransaction(eq(testAccountNumber), any(CreateTransactionRequest.class), eq(testUserId)))
                .thenThrow(new ResourceNotFoundException("Bank account", testAccountNumber));

        // When & Then
        assertThatThrownBy(() -> transactionController.createTransaction(testAccountNumber, authHeader, createTransactionRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bank account")
                .hasMessageContaining(testAccountNumber);

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(transactionService, times(1)).createTransaction(eq(testAccountNumber), any(CreateTransactionRequest.class), eq(testUserId));
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Unauthorized (user does not own account)")
    void testCreateTransaction_Unauthorized() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.createTransaction(eq(testAccountNumber), any(CreateTransactionRequest.class), eq(testUserId)))
                .thenThrow(new UnauthorizedException("User does not have access to this bank account"));

        // When & Then
        assertThatThrownBy(() -> transactionController.createTransaction(testAccountNumber, authHeader, createTransactionRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User does not have access to this bank account");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(transactionService, times(1)).createTransaction(eq(testAccountNumber), any(CreateTransactionRequest.class), eq(testUserId));
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Insufficient funds")
    void testCreateTransaction_InsufficientFunds() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.createTransaction(eq(testAccountNumber), any(CreateTransactionRequest.class), eq(testUserId)))
                .thenThrow(new InsufficientFundsException(testAccountNumber, BigDecimal.valueOf(500.0), BigDecimal.valueOf(100.0)));

        // When & Then
        assertThatThrownBy(() -> transactionController.createTransaction(testAccountNumber, authHeader, createTransactionRequest))
                .isInstanceOf(InsufficientFundsException.class);

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(transactionService, times(1)).createTransaction(eq(testAccountNumber), any(CreateTransactionRequest.class), eq(testUserId));
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Invalid Authorization header")
    void testCreateTransaction_InvalidAuthHeader() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader))
                .thenThrow(new IllegalArgumentException("Access token is missing or invalid"));

        // When & Then
        assertThatThrownBy(() -> transactionController.createTransaction(testAccountNumber, authHeader, createTransactionRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access token is missing or invalid");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(transactionService, never()).createTransaction(anyString(), any(CreateTransactionRequest.class), anyString());
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Verify service is called with correct parameters")
    void testCreateTransaction_VerifyServiceCalledWithCorrectParameters() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.createTransaction(eq(testAccountNumber), any(CreateTransactionRequest.class), eq(testUserId)))
                .thenReturn(transactionResponse);

        // When
        transactionController.createTransaction(testAccountNumber, authHeader, createTransactionRequest);

        // Then - Verify service was called with correct parameters
        verify(transactionService, times(1)).createTransaction(eq(testAccountNumber), argThat(req ->
                req.getAmount().equals(100.0) &&
                req.getCurrency() == BankAccountResponse.Currency.GBP &&
                req.getType() == CreateTransactionRequest.TransactionType.deposit &&
                "Deposit from ATM".equals(req.getReference())
        ), eq(testUserId));
    }

    @Test
    @DisplayName("POST /v1/accounts/{accountNumber}/transactions - Verify response body is properly constructed")
    void testCreateTransaction_VerifyResponseBodyConstruction() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.createTransaction(eq(testAccountNumber), any(CreateTransactionRequest.class), eq(testUserId)))
                .thenReturn(transactionResponse);

        // When
        ResponseEntity<TransactionResponse> response = 
                transactionController.createTransaction(testAccountNumber, authHeader, createTransactionRequest);

        // Then
        assertThat(response.getBody()).isInstanceOf(TransactionResponse.class);
        assertThat(response.getBody().getId()).isEqualTo(testTransactionId);
        assertThat(response.getBody().getAmount()).isEqualTo(100.0);
        assertThat(response.getBody().getType()).isEqualTo(CreateTransactionRequest.TransactionType.deposit);
    }

    // --- listAccountTransaction Tests ---

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions - Success")
    void testListAccountTransaction_Success() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.listTransactions(testAccountNumber, testUserId))
                .thenReturn(listTransactionsResponse);

        // When
        ResponseEntity<ListTransactionsResponse> response = 
                transactionController.listAccountTransaction(testAccountNumber, authHeader);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTransactions()).isNotNull();
        assertThat(response.getBody().getTransactions()).hasSize(1);
        assertThat(response.getBody().getTransactions().get(0).getId()).isEqualTo(testTransactionId);

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(transactionService, times(1)).listTransactions(testAccountNumber, testUserId);
        verify(transactionService, never()).createTransaction(anyString(), any(CreateTransactionRequest.class), anyString());
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions - Account not found")
    void testListAccountTransaction_AccountNotFound() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.listTransactions(testAccountNumber, testUserId))
                .thenThrow(new ResourceNotFoundException("Bank account", testAccountNumber));

        // When & Then
        assertThatThrownBy(() -> transactionController.listAccountTransaction(testAccountNumber, authHeader))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bank account")
                .hasMessageContaining(testAccountNumber);

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(transactionService, times(1)).listTransactions(testAccountNumber, testUserId);
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions - Unauthorized (user does not own account)")
    void testListAccountTransaction_Unauthorized() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.listTransactions(testAccountNumber, testUserId))
                .thenThrow(new UnauthorizedException("User does not have access to this bank account"));

        // When & Then
        assertThatThrownBy(() -> transactionController.listAccountTransaction(testAccountNumber, authHeader))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User does not have access to this bank account");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(transactionService, times(1)).listTransactions(testAccountNumber, testUserId);
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions - Invalid Authorization header")
    void testListAccountTransaction_InvalidAuthHeader() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader))
                .thenThrow(new IllegalArgumentException("Access token is missing or invalid"));

        // When & Then
        assertThatThrownBy(() -> transactionController.listAccountTransaction(testAccountNumber, authHeader))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access token is missing or invalid");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(transactionService, never()).listTransactions(anyString(), anyString());
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions - Success with empty list")
    void testListAccountTransaction_SuccessEmptyList() {
        // Given
        ListTransactionsResponse emptyResponse = new ListTransactionsResponse();
        emptyResponse.setTransactions(List.of());
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.listTransactions(testAccountNumber, testUserId))
                .thenReturn(emptyResponse);

        // When
        ResponseEntity<ListTransactionsResponse> response = 
                transactionController.listAccountTransaction(testAccountNumber, authHeader);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTransactions()).isNotNull();
        assertThat(response.getBody().getTransactions()).isEmpty();
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions - Verify service is called with correct parameters")
    void testListAccountTransaction_VerifyServiceCalledWithCorrectParameters() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.listTransactions(testAccountNumber, testUserId))
                .thenReturn(listTransactionsResponse);

        // When
        transactionController.listAccountTransaction(testAccountNumber, authHeader);

        // Then - Verify service was called with correct accountNumber and userId
        verify(transactionService, times(1)).listTransactions(eq(testAccountNumber), eq(testUserId));
    }

    // --- fetchAccountTransactionByID Tests ---

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Success")
    void testFetchAccountTransactionByID_Success() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.getTransactionById(testAccountNumber, testTransactionId, testUserId))
                .thenReturn(transactionResponse);

        // When
        ResponseEntity<TransactionResponse> response = 
                transactionController.fetchAccountTransactionByID(testAccountNumber, testTransactionId, authHeader);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(testTransactionId);
        assertThat(response.getBody().getAmount()).isEqualTo(100.0);

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(transactionService, times(1)).getTransactionById(testAccountNumber, testTransactionId, testUserId);
        verify(transactionService, never()).createTransaction(anyString(), any(CreateTransactionRequest.class), anyString());
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Account not found")
    void testFetchAccountTransactionByID_AccountNotFound() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.getTransactionById(testAccountNumber, testTransactionId, testUserId))
                .thenThrow(new ResourceNotFoundException("Bank account", testAccountNumber));

        // When & Then
        assertThatThrownBy(() -> transactionController.fetchAccountTransactionByID(testAccountNumber, testTransactionId, authHeader))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bank account")
                .hasMessageContaining(testAccountNumber);

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(transactionService, times(1)).getTransactionById(testAccountNumber, testTransactionId, testUserId);
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Transaction not found")
    void testFetchAccountTransactionByID_TransactionNotFound() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.getTransactionById(testAccountNumber, testTransactionId, testUserId))
                .thenThrow(new ResourceNotFoundException("Transaction", testTransactionId));

        // When & Then
        assertThatThrownBy(() -> transactionController.fetchAccountTransactionByID(testAccountNumber, testTransactionId, authHeader))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction")
                .hasMessageContaining(testTransactionId);

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(transactionService, times(1)).getTransactionById(testAccountNumber, testTransactionId, testUserId);
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Unauthorized (user does not own account)")
    void testFetchAccountTransactionByID_Unauthorized() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.getTransactionById(testAccountNumber, testTransactionId, testUserId))
                .thenThrow(new UnauthorizedException("User does not have access to this bank account"));

        // When & Then
        assertThatThrownBy(() -> transactionController.fetchAccountTransactionByID(testAccountNumber, testTransactionId, authHeader))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User does not have access to this bank account");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(transactionService, times(1)).getTransactionById(testAccountNumber, testTransactionId, testUserId);
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Invalid Authorization header")
    void testFetchAccountTransactionByID_InvalidAuthHeader() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader))
                .thenThrow(new IllegalArgumentException("Access token is missing or invalid"));

        // When & Then
        assertThatThrownBy(() -> transactionController.fetchAccountTransactionByID(testAccountNumber, testTransactionId, authHeader))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access token is missing or invalid");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(transactionService, never()).getTransactionById(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Verify service is called with correct parameters")
    void testFetchAccountTransactionByID_VerifyServiceCalledWithCorrectParameters() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.getTransactionById(testAccountNumber, testTransactionId, testUserId))
                .thenReturn(transactionResponse);

        // When
        transactionController.fetchAccountTransactionByID(testAccountNumber, testTransactionId, authHeader);

        // Then - Verify service was called with correct parameters
        verify(transactionService, times(1)).getTransactionById(eq(testAccountNumber), eq(testTransactionId), eq(testUserId));
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber}/transactions/{transactionId} - Verify response body is properly constructed")
    void testFetchAccountTransactionByID_VerifyResponseBodyConstruction() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(transactionService.getTransactionById(testAccountNumber, testTransactionId, testUserId))
                .thenReturn(transactionResponse);

        // When
        ResponseEntity<TransactionResponse> response = 
                transactionController.fetchAccountTransactionByID(testAccountNumber, testTransactionId, authHeader);

        // Then
        assertThat(response.getBody()).isInstanceOf(TransactionResponse.class);
        assertThat(response.getBody().getId()).isEqualTo(testTransactionId);
        assertThat(response.getBody().getAmount()).isEqualTo(100.0);
        assertThat(response.getBody().getType()).isEqualTo(CreateTransactionRequest.TransactionType.deposit);
    }
}
