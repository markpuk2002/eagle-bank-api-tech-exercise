package com.eaglebank.controller;

import com.eaglebank.dto.request.CreateTransactionRequest;
import com.eaglebank.dto.response.ListTransactionsResponse;
import com.eaglebank.dto.response.TransactionResponse;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.exception.UnauthorizedException;
import com.eaglebank.service.AuthenticationService;
import com.eaglebank.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for transaction management endpoints.
 * Provides operations for creating and retrieving transactions with ownership verification via JWT authentication.
 */
@RestController
@RequestMapping("/v1/accounts/{accountNumber}/transactions")
@Validated
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthenticationService authenticationService;

    /**
     * POST /v1/accounts/{accountNumber}/transactions
     * Creates a new transaction (deposit or withdrawal) for the specified bank account.
     *
     * @param accountNumber The account number for the transaction (format: 01xxxxxx)
     * @param authHeader The Authorization header containing JWT Bearer token
     * @param request The transaction creation request containing amount, type, currency, and reference
     * @return The created transaction response (HTTP 201 Created)
     * @throws ResourceNotFoundException If the bank account does not exist
     * @throws UnauthorizedException If the user does not own the bank account
     * @throws ResponseStatusException If insufficient funds for withdrawal (HTTP 422 Unprocessable Content)
     * @throws IllegalArgumentException If the Authorization header is invalid or validation fails
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @PathVariable
            @Pattern(regexp = "^01\\d{6}$", message = "Account number must match pattern ^01\\d{6}$")
            String accountNumber,
            @RequestHeader(value = "Authorization", required = true) String authHeader,
            @Valid @RequestBody CreateTransactionRequest request) {
        String userId = authenticationService.getUserIdFromHeader(authHeader);
        TransactionResponse response = transactionService.createTransaction(accountNumber, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /v1/accounts/{accountNumber}/transactions
     * Retrieves all transactions for a specific bank account after verifying ownership.
     *
     * @param accountNumber The account number to retrieve transactions for (format: 01xxxxxx)
     * @param authHeader The Authorization header containing JWT Bearer token
     * @return A list of all transactions for the account (HTTP 200 OK)
     * @throws ResourceNotFoundException If the bank account does not exist
     * @throws UnauthorizedException If the user does not own the bank account
     * @throws IllegalArgumentException If the Authorization header is invalid
     */
    @GetMapping
    public ResponseEntity<ListTransactionsResponse> listAccountTransaction(
            @PathVariable
            @Pattern(regexp = "^01\\d{6}$", message = "Account number must match pattern ^01\\d{6}$")
            String accountNumber,
            @RequestHeader(value = "Authorization", required = true) String authHeader) {
        String userId = authenticationService.getUserIdFromHeader(authHeader);
        ListTransactionsResponse response = transactionService.listTransactions(accountNumber, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /v1/accounts/{accountNumber}/transactions/{transactionId}
     * Retrieves a specific transaction by ID after verifying ownership of the account.
     *
     * @param accountNumber The account number the transaction belongs to (format: 01xxxxxx)
     * @param transactionId The transaction ID to retrieve (format: tan-{12 chars})
     * @param authHeader The Authorization header containing JWT Bearer token
     * @return The transaction response (HTTP 200 OK)
     * @throws ResourceNotFoundException If the bank account or transaction does not exist
     * @throws UnauthorizedException If the user does not own the bank account
     * @throws IllegalArgumentException If the Authorization header is invalid
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> fetchAccountTransactionByID(
            @PathVariable
            @Pattern(regexp = "^01\\d{6}$", message = "Account number must match pattern ^01\\d{6}$")
            String accountNumber,
            @PathVariable
            @Pattern(regexp = "^tan-[A-Za-z0-9]{12}$", message = "Transaction ID must match pattern ^tan-[A-Za-z0-9]{12}$")
            String transactionId,
            @RequestHeader(value = "Authorization", required = true) String authHeader) {
        String userId = authenticationService.getUserIdFromHeader(authHeader);
        TransactionResponse response = transactionService.getTransactionById(accountNumber, transactionId, userId);
        return ResponseEntity.ok(response);
    }
}