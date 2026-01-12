package com.eaglebank.controller;

import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.request.CreateBankAccountRequest;
import com.eaglebank.dto.response.ListBankAccountsResponse;
import com.eaglebank.dto.request.UpdateBankAccountRequest;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.exception.UnauthorizedException;
import com.eaglebank.service.AccountService;
import com.eaglebank.service.AuthenticationService;
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

/**
 * REST controller for bank account management endpoints.
 * Provides CRUD operations for bank accounts with ownership verification via JWT authentication.
 */
@RestController
@RequestMapping("/v1/accounts")
@Validated
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AuthenticationService authenticationService;

    /**
     * POST /v1/accounts
     * Creates a new bank account for the authenticated user.
     *
     * @param authHeader The Authorization header containing JWT Bearer token
     * @param request The bank account creation request containing account details
     * @return The created bank account response (HTTP 201 Created)
     * @throws IllegalArgumentException If the Authorization header is invalid or user does not exist
     * @throws ResourceNotFoundException If the user does not exist
     */
    @PostMapping
    public ResponseEntity<BankAccountResponse> createAccount(
            @RequestHeader(value = "Authorization", required = true) String authHeader,
            @Valid @RequestBody CreateBankAccountRequest request) {
        String userId = authenticationService.getUserIdFromHeader(authHeader);
        
        BankAccountResponse response = accountService.createAccount(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /v1/accounts
     * Retrieves all bank accounts for the authenticated user.
     *
     * @param authHeader The Authorization header containing JWT Bearer token
     * @return A list of all bank accounts owned by the user (HTTP 200 OK)
     * @throws IllegalArgumentException If the Authorization header is invalid
     */
    @GetMapping
    public ResponseEntity<ListBankAccountsResponse> listAccounts(
            @RequestHeader(value = "Authorization", required = true) String authHeader) {
        String userId = authenticationService.getUserIdFromHeader(authHeader);
        
        ListBankAccountsResponse response = accountService.listAccounts(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /v1/accounts/{accountNumber}
     * Retrieves a specific bank account by account number after verifying ownership.
     *
     * @param accountNumber The account number to retrieve (format: 01xxxxxx)
     * @param authHeader The Authorization header containing JWT Bearer token
     * @return The bank account response (HTTP 200 OK)
     * @throws ResourceNotFoundException If the bank account does not exist
     * @throws UnauthorizedException If the user does not own the bank account
     * @throws IllegalArgumentException If the Authorization header is invalid
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<BankAccountResponse> fetchAccountByAccountNumber(
            @PathVariable
            @Pattern(regexp = "^01\\d{6}$", message = "Account number must match pattern ^01\\d{6}$")
            String accountNumber,
            @RequestHeader(value = "Authorization", required = true) String authHeader) {
        String userId = authenticationService.getUserIdFromHeader(authHeader);
        BankAccountResponse response = accountService.getAccountByAccountNumber(accountNumber, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /v1/accounts/{accountNumber}
     * Updates a bank account's information after verifying ownership.
     *
     * @param accountNumber The account number to update (format: 01xxxxxx)
     * @param authHeader The Authorization header containing JWT Bearer token
     * @param request The update request containing new values (only non-null fields will be updated)
     * @return The updated bank account response (HTTP 200 OK)
     * @throws ResourceNotFoundException If the bank account does not exist
     * @throws UnauthorizedException If the user does not own the bank account
     * @throws IllegalArgumentException If the Authorization header is invalid or validation fails
     */
    @PatchMapping("/{accountNumber}")
    public ResponseEntity<BankAccountResponse> updateAccountByAccountNumber(
            @PathVariable
            @Pattern(regexp = "^01\\d{6}$", message = "Account number must match pattern ^01\\d{6}$")
            String accountNumber,
            @RequestHeader(value = "Authorization", required = true) String authHeader,
            @Valid @RequestBody UpdateBankAccountRequest request) {
        String userId = authenticationService.getUserIdFromHeader(authHeader);
        BankAccountResponse response = accountService.updateAccount(accountNumber, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /v1/accounts/{accountNumber}
     * Deletes a bank account after verifying ownership.
     *
     * @param accountNumber The account number to delete (format: 01xxxxxx)
     * @param authHeader The Authorization header containing JWT Bearer token
     * @return Empty response (HTTP 204 No Content)
     * @throws ResourceNotFoundException If the bank account does not exist
     * @throws UnauthorizedException If the user does not own the bank account
     * @throws IllegalArgumentException If the Authorization header is invalid
     */
    @DeleteMapping("/{accountNumber}")
    public ResponseEntity<Void> deleteAccountByAccountNumber(
            @PathVariable
            @Pattern(regexp = "^01\\d{6}$", message = "Account number must match pattern ^01\\d{6}$")
            String accountNumber,
            @RequestHeader(value = "Authorization", required = true) String authHeader) {
        String userId = authenticationService.getUserIdFromHeader(authHeader);
        accountService.deleteAccount(accountNumber, userId);
        return ResponseEntity.noContent().build();
    }
}