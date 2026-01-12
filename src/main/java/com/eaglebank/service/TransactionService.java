package com.eaglebank.service;

import com.eaglebank.dto.request.CreateTransactionRequest;
import com.eaglebank.dto.response.ListTransactionsResponse;
import com.eaglebank.dto.response.TransactionResponse;
import com.eaglebank.entity.BankAccount;
import com.eaglebank.entity.Transaction;
import com.eaglebank.entity.User;
import com.eaglebank.exception.InsufficientFundsException;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.exception.UnauthorizedException;
import com.eaglebank.util.EnumConverter;
import com.eaglebank.mapper.DtoMapper;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.TransactionRepository;
import com.eaglebank.util.AuthorizationHelper;
import com.eaglebank.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for managing transactions.
 * Handles transaction creation, retrieval, and balance updates with proper locking and authorization.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final DtoMapper dtoMapper;
    private final IdGenerator idGenerator;

    /**
     * Creates a new transaction (deposit or withdrawal) for the specified bank account.
     * Uses pessimistic locking to prevent concurrent balance modifications.
     *
     * @param accountNumber The account number for the transaction (format: 01xxxxxx)
     * @param request The transaction creation request containing amount, type, currency, and reference
     * @param userId The ID of the user making the request (must own the account)
     * @return The created transaction response with generated transaction ID
     * @throws ResourceNotFoundException If the bank account with the specified account number does not exist
     * @throws UnauthorizedException If the user does not own the bank account
     * @throws InsufficientFundsException If insufficient funds for withdrawal
     * @throws com.eaglebank.exception.ValidationException If invalid transaction type or currency is provided
     */
    public TransactionResponse createTransaction(String accountNumber, CreateTransactionRequest request, String userId) {
        BankAccount account = bankAccountRepository.findByAccountNumberWithLock(accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Bank account", accountNumber));

        User user = account.getUser();
        AuthorizationHelper.verifyUserOwnership(user.getId(), userId, "bank account");

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());
        
        Transaction.TransactionType type = EnumConverter.convertEnum(
            Transaction.TransactionType.class,
            request.getType(),
            "transaction type"
        );

        BankAccount.Currency currency = EnumConverter.convertEnum(
            BankAccount.Currency.class,
            request.getCurrency(),
            "currency"
        );

        if (type == Transaction.TransactionType.withdrawal) {
            if (account.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(accountNumber, amount, account.getBalance());
            }
        }

        String transactionId = idGenerator.generateTransactionId();

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setBankAccount(account);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setType(type);
        transaction.setReference(request.getReference());
        transaction.setUser(user);

        if (type == Transaction.TransactionType.deposit) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            account.setBalance(account.getBalance().subtract(amount));
        }

        bankAccountRepository.save(account);
        Transaction savedTransaction = transactionRepository.save(transaction);

        log.info("Transaction created successfully: {} (type: {}, amount: {}, account: {})", 
            transactionId, type, amount, accountNumber);
        return dtoMapper.toTransactionResponse(savedTransaction);
    }

    /**
     * Retrieves all transactions for a specific bank account after verifying user ownership.
     *
     * @param accountNumber The account number to retrieve transactions for (format: 01xxxxxx)
     * @param userId The ID of the user making the request (must own the account)
     * @return A list of all transactions for the account
     * @throws ResourceNotFoundException If the bank account with the specified account number does not exist
     * @throws UnauthorizedException If the user does not own the bank account
     */
    @Transactional(readOnly = true)
    public ListTransactionsResponse listTransactions(String accountNumber, String userId) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Bank account", accountNumber));
        
        AuthorizationHelper.verifyUserOwnership(account.getUser().getId(), userId, "bank account");
        
        List<Transaction> transactions = transactionRepository.findByBankAccount_AccountNumber(accountNumber);
        ListTransactionsResponse response = new ListTransactionsResponse();
        response.setTransactions(dtoMapper.toTransactionResponseList(transactions));
        return response;
    }

    /**
     * Retrieves a specific transaction by ID after verifying user ownership of the account.
     *
     * @param accountNumber The account number the transaction belongs to (format: 01xxxxxx)
     * @param transactionId The transaction ID to retrieve (format: tan-{12 chars})
     * @param userId The ID of the user making the request (must own the account)
     * @return The transaction response
     * @throws ResourceNotFoundException If the bank account or transaction does not exist
     * @throws UnauthorizedException If the user does not own the bank account
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(String accountNumber, String transactionId, String userId) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Bank account", accountNumber));
        
        AuthorizationHelper.verifyUserOwnership(account.getUser().getId(), userId, "bank account");
        
        return transactionRepository.findByIdAndBankAccount_AccountNumber(transactionId, accountNumber)
            .map(dtoMapper::toTransactionResponse)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Transaction", transactionId + " for account: " + accountNumber
            ));
    }
}