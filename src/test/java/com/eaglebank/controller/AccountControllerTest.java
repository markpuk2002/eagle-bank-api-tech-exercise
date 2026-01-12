package com.eaglebank.controller;

import com.eaglebank.constants.ApiConstants;
import com.eaglebank.dto.request.CreateBankAccountRequest;
import com.eaglebank.dto.request.UpdateBankAccountRequest;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.response.ListBankAccountsResponse;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.exception.UnauthorizedException;
import com.eaglebank.service.AccountService;
import com.eaglebank.service.AuthenticationService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Controller Unit Tests")
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AccountController accountController;

    private CreateBankAccountRequest createBankAccountRequest;
    private UpdateBankAccountRequest updateBankAccountRequest;
    private BankAccountResponse bankAccountResponse;
    private ListBankAccountsResponse listBankAccountsResponse;
    private String testUserId;
    private String testAccountNumber;
    private String authHeader;

    @BeforeEach
    void setUp() {
        testUserId = "usr-abcdefghijkl";
        testAccountNumber = "01123456";
        authHeader = ApiConstants.BEARER_PREFIX + "test-jwt-token";

        createBankAccountRequest = new CreateBankAccountRequest();
        createBankAccountRequest.setName("My Savings Account");
        createBankAccountRequest.setAccountType(CreateBankAccountRequest.AccountType.personal);

        updateBankAccountRequest = new UpdateBankAccountRequest();
        updateBankAccountRequest.setName("My Updated Account");
        updateBankAccountRequest.setAccountType(CreateBankAccountRequest.AccountType.personal);

        bankAccountResponse = new BankAccountResponse();
        bankAccountResponse.setAccountNumber(testAccountNumber);
        bankAccountResponse.setSortCode("10-10-10");
        bankAccountResponse.setName("My Savings Account");
        bankAccountResponse.setAccountType(CreateBankAccountRequest.AccountType.personal);
        bankAccountResponse.setBalance(1000.0);
        bankAccountResponse.setCurrency(BankAccountResponse.Currency.GBP);
        bankAccountResponse.setCreatedTimestamp(OffsetDateTime.now());
        bankAccountResponse.setUpdatedTimestamp(OffsetDateTime.now());

        listBankAccountsResponse = new ListBankAccountsResponse();
        listBankAccountsResponse.setAccounts(List.of(bankAccountResponse));
    }

    // --- createAccount Tests ---

    @Test
    @DisplayName("POST /v1/accounts - Success")
    void testCreateAccount_Success() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(accountService.createAccount(any(CreateBankAccountRequest.class), eq(testUserId)))
                .thenReturn(bankAccountResponse);

        // When
        ResponseEntity<BankAccountResponse> response = accountController.createAccount(authHeader, createBankAccountRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccountNumber()).isEqualTo(testAccountNumber);
        assertThat(response.getBody().getName()).isEqualTo("My Savings Account");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, times(1)).createAccount(any(CreateBankAccountRequest.class), eq(testUserId));
        verify(accountService, never()).listAccounts(anyString());
        verify(accountService, never()).getAccountByAccountNumber(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /v1/accounts - Invalid Authorization header")
    void testCreateAccount_InvalidAuthHeader() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader))
                .thenThrow(new IllegalArgumentException("Access token is missing or invalid"));

        // When & Then
        assertThatThrownBy(() -> accountController.createAccount(authHeader, createBankAccountRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access token is missing or invalid");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, never()).createAccount(any(CreateBankAccountRequest.class), anyString());
    }

    @Test
    @DisplayName("POST /v1/accounts - Verify service is called with correct parameters")
    void testCreateAccount_VerifyServiceCalledWithCorrectParameters() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(accountService.createAccount(any(CreateBankAccountRequest.class), eq(testUserId)))
                .thenReturn(bankAccountResponse);

        // When
        accountController.createAccount(authHeader, createBankAccountRequest);

        // Then - Verify service was called with correct request and userId
        verify(accountService, times(1)).createAccount(argThat(req ->
                req.getName().equals("My Savings Account") &&
                req.getAccountType() == CreateBankAccountRequest.AccountType.personal
        ), eq(testUserId));
    }

    @Test
    @DisplayName("POST /v1/accounts - Verify response body is properly constructed")
    void testCreateAccount_VerifyResponseBodyConstruction() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(accountService.createAccount(any(CreateBankAccountRequest.class), eq(testUserId)))
                .thenReturn(bankAccountResponse);

        // When
        ResponseEntity<BankAccountResponse> response = accountController.createAccount(authHeader, createBankAccountRequest);

        // Then
        assertThat(response.getBody()).isInstanceOf(BankAccountResponse.class);
        assertThat(response.getBody().getAccountNumber()).isEqualTo(testAccountNumber);
        assertThat(response.getBody().getName()).isEqualTo("My Savings Account");
        assertThat(response.getBody().getAccountType()).isEqualTo(CreateBankAccountRequest.AccountType.personal);
    }

    // --- listAccounts Tests ---

    @Test
    @DisplayName("GET /v1/accounts - Success")
    void testListAccounts_Success() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(accountService.listAccounts(testUserId)).thenReturn(listBankAccountsResponse);

        // When
        ResponseEntity<ListBankAccountsResponse> response = accountController.listAccounts(authHeader);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccounts()).isNotNull();
        assertThat(response.getBody().getAccounts()).hasSize(1);
        assertThat(response.getBody().getAccounts().get(0).getAccountNumber()).isEqualTo(testAccountNumber);

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, times(1)).listAccounts(testUserId);
        verify(accountService, never()).createAccount(any(CreateBankAccountRequest.class), anyString());
    }

    @Test
    @DisplayName("GET /v1/accounts - Invalid Authorization header")
    void testListAccounts_InvalidAuthHeader() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader))
                .thenThrow(new IllegalArgumentException("Access token is missing or invalid"));

        // When & Then
        assertThatThrownBy(() -> accountController.listAccounts(authHeader))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access token is missing or invalid");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, never()).listAccounts(anyString());
    }

    @Test
    @DisplayName("GET /v1/accounts - Verify service is called with correct userId")
    void testListAccounts_VerifyServiceCalledWithCorrectUserId() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(accountService.listAccounts(testUserId)).thenReturn(listBankAccountsResponse);

        // When
        accountController.listAccounts(authHeader);

        // Then - Verify service was called with correct userId
        verify(accountService, times(1)).listAccounts(eq(testUserId));
    }

    // --- fetchAccountByAccountNumber Tests ---

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber} - Success")
    void testFetchAccountByAccountNumber_Success() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(accountService.getAccountByAccountNumber(testAccountNumber, testUserId))
                .thenReturn(bankAccountResponse);

        // When
        ResponseEntity<BankAccountResponse> response = 
                accountController.fetchAccountByAccountNumber(testAccountNumber, authHeader);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccountNumber()).isEqualTo(testAccountNumber);

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, times(1)).getAccountByAccountNumber(testAccountNumber, testUserId);
        verify(accountService, never()).updateAccount(anyString(), any(UpdateBankAccountRequest.class), anyString());
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber} - Account not found")
    void testFetchAccountByAccountNumber_AccountNotFound() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(accountService.getAccountByAccountNumber(testAccountNumber, testUserId))
                .thenThrow(new ResourceNotFoundException("BankAccount", testAccountNumber));

        // When & Then
        assertThatThrownBy(() -> accountController.fetchAccountByAccountNumber(testAccountNumber, authHeader))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("BankAccount")
                .hasMessageContaining(testAccountNumber);

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, times(1)).getAccountByAccountNumber(testAccountNumber, testUserId);
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber} - Unauthorized (user does not own account)")
    void testFetchAccountByAccountNumber_Unauthorized() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(accountService.getAccountByAccountNumber(testAccountNumber, testUserId))
                .thenThrow(new UnauthorizedException("User does not have access to this bank account"));

        // When & Then
        assertThatThrownBy(() -> accountController.fetchAccountByAccountNumber(testAccountNumber, authHeader))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User does not have access to this bank account");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, times(1)).getAccountByAccountNumber(testAccountNumber, testUserId);
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber} - Invalid Authorization header")
    void testFetchAccountByAccountNumber_InvalidAuthHeader() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader))
                .thenThrow(new IllegalArgumentException("Access token is missing or invalid"));

        // When & Then
        assertThatThrownBy(() -> accountController.fetchAccountByAccountNumber(testAccountNumber, authHeader))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access token is missing or invalid");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, never()).getAccountByAccountNumber(anyString(), anyString());
    }

    @Test
    @DisplayName("GET /v1/accounts/{accountNumber} - Verify service is called with correct parameters")
    void testFetchAccountByAccountNumber_VerifyServiceCalledWithCorrectParameters() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(accountService.getAccountByAccountNumber(testAccountNumber, testUserId))
                .thenReturn(bankAccountResponse);

        // When
        accountController.fetchAccountByAccountNumber(testAccountNumber, authHeader);

        // Then - Verify service was called with correct accountNumber and userId
        verify(accountService, times(1)).getAccountByAccountNumber(eq(testAccountNumber), eq(testUserId));
    }

    // --- updateAccountByAccountNumber Tests ---

    @Test
    @DisplayName("PATCH /v1/accounts/{accountNumber} - Success")
    void testUpdateAccountByAccountNumber_Success() {
        // Given
        BankAccountResponse updatedResponse = new BankAccountResponse();
        updatedResponse.setAccountNumber(testAccountNumber);
        updatedResponse.setName("My Updated Account");
        updatedResponse.setAccountType(CreateBankAccountRequest.AccountType.personal);

        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(accountService.updateAccount(eq(testAccountNumber), any(UpdateBankAccountRequest.class), eq(testUserId)))
                .thenReturn(updatedResponse);

        // When
        ResponseEntity<BankAccountResponse> response = 
                accountController.updateAccountByAccountNumber(testAccountNumber, authHeader, updateBankAccountRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccountNumber()).isEqualTo(testAccountNumber);
        assertThat(response.getBody().getName()).isEqualTo("My Updated Account");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, times(1)).updateAccount(eq(testAccountNumber), any(UpdateBankAccountRequest.class), eq(testUserId));
        verify(accountService, never()).deleteAccount(anyString(), anyString());
    }

    @Test
    @DisplayName("PATCH /v1/accounts/{accountNumber} - Account not found")
    void testUpdateAccountByAccountNumber_AccountNotFound() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(accountService.updateAccount(eq(testAccountNumber), any(UpdateBankAccountRequest.class), eq(testUserId)))
                .thenThrow(new ResourceNotFoundException("BankAccount", testAccountNumber));

        // When & Then
        assertThatThrownBy(() -> accountController.updateAccountByAccountNumber(testAccountNumber, authHeader, updateBankAccountRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("BankAccount")
                .hasMessageContaining(testAccountNumber);

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, times(1)).updateAccount(eq(testAccountNumber), any(UpdateBankAccountRequest.class), eq(testUserId));
    }

    @Test
    @DisplayName("PATCH /v1/accounts/{accountNumber} - Unauthorized (user does not own account)")
    void testUpdateAccountByAccountNumber_Unauthorized() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(accountService.updateAccount(eq(testAccountNumber), any(UpdateBankAccountRequest.class), eq(testUserId)))
                .thenThrow(new UnauthorizedException("User does not have access to this bank account"));

        // When & Then
        assertThatThrownBy(() -> accountController.updateAccountByAccountNumber(testAccountNumber, authHeader, updateBankAccountRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User does not have access to this bank account");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, times(1)).updateAccount(eq(testAccountNumber), any(UpdateBankAccountRequest.class), eq(testUserId));
    }

    @Test
    @DisplayName("PATCH /v1/accounts/{accountNumber} - Invalid Authorization header")
    void testUpdateAccountByAccountNumber_InvalidAuthHeader() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader))
                .thenThrow(new IllegalArgumentException("Access token is missing or invalid"));

        // When & Then
        assertThatThrownBy(() -> accountController.updateAccountByAccountNumber(testAccountNumber, authHeader, updateBankAccountRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access token is missing or invalid");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, never()).updateAccount(anyString(), any(UpdateBankAccountRequest.class), anyString());
    }

    @Test
    @DisplayName("PATCH /v1/accounts/{accountNumber} - Verify service is called with correct parameters")
    void testUpdateAccountByAccountNumber_VerifyServiceCalledWithCorrectParameters() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        when(accountService.updateAccount(eq(testAccountNumber), any(UpdateBankAccountRequest.class), eq(testUserId)))
                .thenReturn(bankAccountResponse);

        // When
        accountController.updateAccountByAccountNumber(testAccountNumber, authHeader, updateBankAccountRequest);

        // Then - Verify service was called with correct parameters
        verify(accountService, times(1)).updateAccount(eq(testAccountNumber), argThat(req ->
                req.getName().equals("My Updated Account") &&
                req.getAccountType() == CreateBankAccountRequest.AccountType.personal
        ), eq(testUserId));
    }

    // --- deleteAccountByAccountNumber Tests ---

    @Test
    @DisplayName("DELETE /v1/accounts/{accountNumber} - Success")
    void testDeleteAccountByAccountNumber_Success() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        doNothing().when(accountService).deleteAccount(testAccountNumber, testUserId);

        // When
        ResponseEntity<Void> response = accountController.deleteAccountByAccountNumber(testAccountNumber, authHeader);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, times(1)).deleteAccount(testAccountNumber, testUserId);
        verify(accountService, never()).createAccount(any(CreateBankAccountRequest.class), anyString());
    }

    @Test
    @DisplayName("DELETE /v1/accounts/{accountNumber} - Account not found")
    void testDeleteAccountByAccountNumber_AccountNotFound() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        doThrow(new ResourceNotFoundException("BankAccount", testAccountNumber))
                .when(accountService).deleteAccount(testAccountNumber, testUserId);

        // When & Then
        assertThatThrownBy(() -> accountController.deleteAccountByAccountNumber(testAccountNumber, authHeader))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("BankAccount")
                .hasMessageContaining(testAccountNumber);

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, times(1)).deleteAccount(testAccountNumber, testUserId);
    }

    @Test
    @DisplayName("DELETE /v1/accounts/{accountNumber} - Unauthorized (user does not own account)")
    void testDeleteAccountByAccountNumber_Unauthorized() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        doThrow(new UnauthorizedException("User does not have access to this bank account"))
                .when(accountService).deleteAccount(testAccountNumber, testUserId);

        // When & Then
        assertThatThrownBy(() -> accountController.deleteAccountByAccountNumber(testAccountNumber, authHeader))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User does not have access to this bank account");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, times(1)).deleteAccount(testAccountNumber, testUserId);
    }

    @Test
    @DisplayName("DELETE /v1/accounts/{accountNumber} - Invalid Authorization header")
    void testDeleteAccountByAccountNumber_InvalidAuthHeader() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader))
                .thenThrow(new IllegalArgumentException("Access token is missing or invalid"));

        // When & Then
        assertThatThrownBy(() -> accountController.deleteAccountByAccountNumber(testAccountNumber, authHeader))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access token is missing or invalid");

        verify(authenticationService, times(1)).getUserIdFromHeader(authHeader);
        verify(accountService, never()).deleteAccount(anyString(), anyString());
    }

    @Test
    @DisplayName("DELETE /v1/accounts/{accountNumber} - Verify service is called with correct parameters")
    void testDeleteAccountByAccountNumber_VerifyServiceCalledWithCorrectParameters() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        doNothing().when(accountService).deleteAccount(testAccountNumber, testUserId);

        // When
        accountController.deleteAccountByAccountNumber(testAccountNumber, authHeader);

        // Then - Verify service was called with correct accountNumber and userId
        verify(accountService, times(1)).deleteAccount(eq(testAccountNumber), eq(testUserId));
    }

    @Test
    @DisplayName("DELETE /v1/accounts/{accountNumber} - Verify response is 204 No Content with null body")
    void testDeleteAccountByAccountNumber_VerifyResponseFormat() {
        // Given
        when(authenticationService.getUserIdFromHeader(authHeader)).thenReturn(testUserId);
        doNothing().when(accountService).deleteAccount(testAccountNumber, testUserId);

        // When
        ResponseEntity<Void> response = accountController.deleteAccountByAccountNumber(testAccountNumber, authHeader);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }
}
