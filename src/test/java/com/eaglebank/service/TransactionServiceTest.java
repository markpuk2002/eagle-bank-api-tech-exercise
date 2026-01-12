package com.eaglebank.service;

import com.eaglebank.dto.request.CreateTransactionRequest;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.response.ListTransactionsResponse;
import com.eaglebank.dto.response.TransactionResponse;
import com.eaglebank.entity.BankAccount;
import com.eaglebank.entity.Transaction;
import com.eaglebank.entity.User;
import com.eaglebank.exception.InsufficientFundsException;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.exception.UnauthorizedException;
import com.eaglebank.mapper.DtoMapper;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.TransactionRepository;
import com.eaglebank.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Service Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private TransactionService transactionService;

    private CreateTransactionRequest createTransactionRequest;
    private TransactionResponse transactionResponse;
    private Transaction testTransaction;
    private BankAccount testBankAccount;
    private User testUser;
    private String testUserId;
    private String testAccountNumber;
    private String testTransactionId;
    private String otherUserId;

    @BeforeEach
    void setUp() {
        testUserId = "usr-abcdefghijkl";
        otherUserId = "usr-xyz123456789";
        testAccountNumber = "01123456";
        testTransactionId = "tan-abcdefghijkl";

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testBankAccount = new BankAccount();
        testBankAccount.setAccountNumber(testAccountNumber);
        testBankAccount.setSortCode("10-10-10");
        testBankAccount.setName("My Savings Account");
        testBankAccount.setAccountType(BankAccount.AccountType.personal);
        testBankAccount.setBalance(BigDecimal.valueOf(1000.0));
        testBankAccount.setCurrency(BankAccount.Currency.GBP);
        testBankAccount.setUser(testUser);

        testTransaction = new Transaction();
        testTransaction.setId(testTransactionId);
        testTransaction.setBankAccount(testBankAccount);
        testTransaction.setAmount(BigDecimal.valueOf(100.0));
        testTransaction.setCurrency(BankAccount.Currency.GBP);
        testTransaction.setType(Transaction.TransactionType.deposit);
        testTransaction.setReference("Deposit from ATM");
        testTransaction.setUser(testUser);
        testTransaction.setCreatedTimestamp(OffsetDateTime.now());

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
    }

    // --- createTransaction Tests ---

    @Test
    @DisplayName("createTransaction - Success (deposit)")
    void testCreateTransaction_SuccessDeposit() {
        // Given
        when(bankAccountRepository.findByAccountNumberWithLock(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(idGenerator.generateTransactionId()).thenReturn(testTransactionId);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(dtoMapper.toTransactionResponse(testTransaction)).thenReturn(transactionResponse);

        // When
        TransactionResponse response = transactionService.createTransaction(
                testAccountNumber, createTransactionRequest, testUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testTransactionId);
        assertThat(response.getAmount()).isEqualTo(100.0);
        verify(bankAccountRepository, times(1)).findByAccountNumberWithLock(testAccountNumber);
        verify(idGenerator, times(1)).generateTransactionId();
        verify(bankAccountRepository, times(1)).save(any(BankAccount.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(dtoMapper, times(1)).toTransactionResponse(testTransaction);
    }

    @Test
    @DisplayName("createTransaction - Success (withdrawal)")
    void testCreateTransaction_SuccessWithdrawal() {
        // Given
        createTransactionRequest.setType(CreateTransactionRequest.TransactionType.withdrawal);
        testBankAccount.setBalance(BigDecimal.valueOf(1000.0)); // Sufficient balance
        testTransaction.setType(Transaction.TransactionType.withdrawal);

        TransactionResponse withdrawalResponse = new TransactionResponse();
        withdrawalResponse.setId(testTransactionId);
        withdrawalResponse.setAmount(100.0);
        withdrawalResponse.setCurrency(BankAccountResponse.Currency.GBP);
        withdrawalResponse.setType(CreateTransactionRequest.TransactionType.withdrawal);
        withdrawalResponse.setReference("Deposit from ATM");
        withdrawalResponse.setUserId(testUserId);
        withdrawalResponse.setCreatedTimestamp(OffsetDateTime.now());

        when(bankAccountRepository.findByAccountNumberWithLock(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(idGenerator.generateTransactionId()).thenReturn(testTransactionId);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(dtoMapper.toTransactionResponse(testTransaction)).thenReturn(withdrawalResponse);

        // When
        TransactionResponse response = transactionService.createTransaction(
                testAccountNumber, createTransactionRequest, testUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(CreateTransactionRequest.TransactionType.withdrawal);
        verify(bankAccountRepository, times(1)).findByAccountNumberWithLock(testAccountNumber);
        verify(idGenerator, times(1)).generateTransactionId();
        verify(bankAccountRepository, times(1)).save(any(BankAccount.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("createTransaction - Account not found")
    void testCreateTransaction_AccountNotFound() {
        // Given
        when(bankAccountRepository.findByAccountNumberWithLock(testAccountNumber))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.createTransaction(
                testAccountNumber, createTransactionRequest, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bank account")
                .hasMessageContaining(testAccountNumber);

        verify(bankAccountRepository, times(1)).findByAccountNumberWithLock(testAccountNumber);
        verify(idGenerator, never()).generateTransactionId();
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("createTransaction - Unauthorized (user does not own account)")
    void testCreateTransaction_Unauthorized() {
        // Given
        User otherUser = new User();
        otherUser.setId(otherUserId);
        BankAccount otherUserAccount = new BankAccount();
        otherUserAccount.setAccountNumber(testAccountNumber);
        otherUserAccount.setUser(otherUser);

        when(bankAccountRepository.findByAccountNumberWithLock(testAccountNumber))
                .thenReturn(Optional.of(otherUserAccount));

        // When & Then - Should throw UnauthorizedException via AuthorizationHelper
        assertThatThrownBy(() -> transactionService.createTransaction(
                testAccountNumber, createTransactionRequest, testUserId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("bank account");

        verify(bankAccountRepository, times(1)).findByAccountNumberWithLock(testAccountNumber);
        verify(idGenerator, never()).generateTransactionId();
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("createTransaction - Insufficient funds for withdrawal")
    void testCreateTransaction_InsufficientFunds() {
        // Given
        createTransactionRequest.setType(CreateTransactionRequest.TransactionType.withdrawal);
        createTransactionRequest.setAmount(500.0);
        testBankAccount.setBalance(BigDecimal.valueOf(100.0)); // Insufficient balance

        when(bankAccountRepository.findByAccountNumberWithLock(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));

        // When & Then
        assertThatThrownBy(() -> transactionService.createTransaction(
                testAccountNumber, createTransactionRequest, testUserId))
                .isInstanceOf(InsufficientFundsException.class);

        verify(bankAccountRepository, times(1)).findByAccountNumberWithLock(testAccountNumber);
        verify(idGenerator, never()).generateTransactionId();
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("createTransaction - Verify deposit updates balance correctly")
    void testCreateTransaction_VerifyDepositBalanceUpdate() {
        // Given
        BigDecimal initialBalance = BigDecimal.valueOf(1000.0);
        BigDecimal depositAmount = BigDecimal.valueOf(100.0);
        testBankAccount.setBalance(initialBalance);
        createTransactionRequest.setAmount(depositAmount.doubleValue());

        when(bankAccountRepository.findByAccountNumberWithLock(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(idGenerator.generateTransactionId()).thenReturn(testTransactionId);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(dtoMapper.toTransactionResponse(any(Transaction.class))).thenReturn(transactionResponse);

        // When
        transactionService.createTransaction(testAccountNumber, createTransactionRequest, testUserId);

        // Then - Verify balance is increased for deposit
        verify(bankAccountRepository, times(1)).save(argThat(account ->
                account.getBalance().compareTo(initialBalance.add(depositAmount)) == 0
        ));
    }

    @Test
    @DisplayName("createTransaction - Verify withdrawal updates balance correctly")
    void testCreateTransaction_VerifyWithdrawalBalanceUpdate() {
        // Given
        BigDecimal initialBalance = BigDecimal.valueOf(1000.0);
        BigDecimal withdrawalAmount = BigDecimal.valueOf(100.0);
        testBankAccount.setBalance(initialBalance);
        createTransactionRequest.setType(CreateTransactionRequest.TransactionType.withdrawal);
        createTransactionRequest.setAmount(withdrawalAmount.doubleValue());
        testTransaction.setType(Transaction.TransactionType.withdrawal);

        TransactionResponse withdrawalResponse = new TransactionResponse();
        withdrawalResponse.setId(testTransactionId);
        withdrawalResponse.setAmount(withdrawalAmount.doubleValue());
        withdrawalResponse.setCurrency(BankAccountResponse.Currency.GBP);
        withdrawalResponse.setType(CreateTransactionRequest.TransactionType.withdrawal);
        withdrawalResponse.setReference("Deposit from ATM");
        withdrawalResponse.setUserId(testUserId);
        withdrawalResponse.setCreatedTimestamp(OffsetDateTime.now());

        when(bankAccountRepository.findByAccountNumberWithLock(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(idGenerator.generateTransactionId()).thenReturn(testTransactionId);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(dtoMapper.toTransactionResponse(any(Transaction.class))).thenReturn(withdrawalResponse);

        // When
        transactionService.createTransaction(testAccountNumber, createTransactionRequest, testUserId);

        // Then - Verify balance is decreased for withdrawal
        verify(bankAccountRepository, times(1)).save(argThat(account ->
                account.getBalance().compareTo(initialBalance.subtract(withdrawalAmount)) == 0
        ));
    }

    @Test
    @DisplayName("createTransaction - Verify transaction entity is built correctly")
    void testCreateTransaction_VerifyTransactionEntityBuiltCorrectly() {
        // Given
        when(bankAccountRepository.findByAccountNumberWithLock(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(idGenerator.generateTransactionId()).thenReturn(testTransactionId);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(dtoMapper.toTransactionResponse(any(Transaction.class))).thenReturn(transactionResponse);

        // When
        transactionService.createTransaction(testAccountNumber, createTransactionRequest, testUserId);

        // Then - Verify transaction entity is saved with correct values
        verify(transactionRepository, times(1)).save(argThat(transaction ->
                testTransactionId.equals(transaction.getId()) &&
                testBankAccount.equals(transaction.getBankAccount()) &&
                BigDecimal.valueOf(100.0).equals(transaction.getAmount()) &&
                BankAccount.Currency.GBP == transaction.getCurrency() &&
                Transaction.TransactionType.deposit == transaction.getType() &&
                "Deposit from ATM".equals(transaction.getReference()) &&
                testUser.equals(transaction.getUser())
        ));
    }

    @Test
    @DisplayName("createTransaction - Verify transaction ID is generated")
    void testCreateTransaction_VerifyTransactionIdGenerated() {
        // Given
        when(bankAccountRepository.findByAccountNumberWithLock(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(idGenerator.generateTransactionId()).thenReturn(testTransactionId);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(dtoMapper.toTransactionResponse(any(Transaction.class))).thenReturn(transactionResponse);

        // When
        transactionService.createTransaction(testAccountNumber, createTransactionRequest, testUserId);

        // Then - Verify transaction ID generator is called
        verify(idGenerator, times(1)).generateTransactionId();
        verify(transactionRepository, times(1)).save(argThat(transaction ->
                testTransactionId.equals(transaction.getId())
        ));
    }

    // --- listTransactions Tests ---

    @Test
    @DisplayName("listTransactions - Success with transactions")
    void testListTransactions_Success() {
        // Given
        List<Transaction> transactions = List.of(testTransaction);
        List<TransactionResponse> transactionResponses = List.of(transactionResponse);
        when(bankAccountRepository.findByAccountNumber(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(transactionRepository.findByBankAccount_AccountNumber(testAccountNumber))
                .thenReturn(transactions);
        when(dtoMapper.toTransactionResponseList(transactions)).thenReturn(transactionResponses);

        // When
        ListTransactionsResponse response = transactionService.listTransactions(testAccountNumber, testUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransactions()).isNotNull();
        assertThat(response.getTransactions()).hasSize(1);
        assertThat(response.getTransactions().get(0).getId()).isEqualTo(testTransactionId);
        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(transactionRepository, times(1)).findByBankAccount_AccountNumber(testAccountNumber);
        verify(dtoMapper, times(1)).toTransactionResponseList(transactions);
    }

    @Test
    @DisplayName("listTransactions - Success with empty list")
    void testListTransactions_SuccessEmptyList() {
        // Given
        List<Transaction> transactions = List.of();
        List<TransactionResponse> transactionResponses = List.of();
        when(bankAccountRepository.findByAccountNumber(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(transactionRepository.findByBankAccount_AccountNumber(testAccountNumber))
                .thenReturn(transactions);
        when(dtoMapper.toTransactionResponseList(transactions)).thenReturn(transactionResponses);

        // When
        ListTransactionsResponse response = transactionService.listTransactions(testAccountNumber, testUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransactions()).isNotNull();
        assertThat(response.getTransactions()).isEmpty();
        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(transactionRepository, times(1)).findByBankAccount_AccountNumber(testAccountNumber);
    }

    @Test
    @DisplayName("listTransactions - Account not found")
    void testListTransactions_AccountNotFound() {
        // Given
        when(bankAccountRepository.findByAccountNumber(testAccountNumber))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.listTransactions(testAccountNumber, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bank account")
                .hasMessageContaining(testAccountNumber);

        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(transactionRepository, never()).findByBankAccount_AccountNumber(anyString());
    }

    @Test
    @DisplayName("listTransactions - Unauthorized (user does not own account)")
    void testListTransactions_Unauthorized() {
        // Given
        User otherUser = new User();
        otherUser.setId(otherUserId);
        BankAccount otherUserAccount = new BankAccount();
        otherUserAccount.setAccountNumber(testAccountNumber);
        otherUserAccount.setUser(otherUser);

        when(bankAccountRepository.findByAccountNumber(testAccountNumber))
                .thenReturn(Optional.of(otherUserAccount));

        // When & Then - Should throw UnauthorizedException via AuthorizationHelper
        assertThatThrownBy(() -> transactionService.listTransactions(testAccountNumber, testUserId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("bank account");

        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(transactionRepository, never()).findByBankAccount_AccountNumber(anyString());
    }

    @Test
    @DisplayName("listTransactions - Verify service is called with correct accountNumber")
    void testListTransactions_VerifyServiceCalledWithCorrectAccountNumber() {
        // Given
        when(bankAccountRepository.findByAccountNumber(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(transactionRepository.findByBankAccount_AccountNumber(testAccountNumber))
                .thenReturn(List.of());
        when(dtoMapper.toTransactionResponseList(any())).thenReturn(List.of());

        // When
        transactionService.listTransactions(testAccountNumber, testUserId);

        // Then - Verify repository was called with correct accountNumber
        verify(transactionRepository, times(1)).findByBankAccount_AccountNumber(eq(testAccountNumber));
    }

    // --- getTransactionById Tests ---

    @Test
    @DisplayName("getTransactionById - Success")
    void testGetTransactionById_Success() {
        // Given
        when(bankAccountRepository.findByAccountNumber(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(transactionRepository.findByIdAndBankAccount_AccountNumber(testTransactionId, testAccountNumber))
                .thenReturn(Optional.of(testTransaction));
        when(dtoMapper.toTransactionResponse(testTransaction)).thenReturn(transactionResponse);

        // When
        TransactionResponse response = transactionService.getTransactionById(
                testAccountNumber, testTransactionId, testUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testTransactionId);
        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(transactionRepository, times(1)).findByIdAndBankAccount_AccountNumber(testTransactionId, testAccountNumber);
        verify(dtoMapper, times(1)).toTransactionResponse(testTransaction);
    }

    @Test
    @DisplayName("getTransactionById - Account not found")
    void testGetTransactionById_AccountNotFound() {
        // Given
        when(bankAccountRepository.findByAccountNumber(testAccountNumber))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getTransactionById(
                testAccountNumber, testTransactionId, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bank account")
                .hasMessageContaining(testAccountNumber);

        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(transactionRepository, never()).findByIdAndBankAccount_AccountNumber(anyString(), anyString());
    }

    @Test
    @DisplayName("getTransactionById - Transaction not found")
    void testGetTransactionById_TransactionNotFound() {
        // Given
        when(bankAccountRepository.findByAccountNumber(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(transactionRepository.findByIdAndBankAccount_AccountNumber(testTransactionId, testAccountNumber))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getTransactionById(
                testAccountNumber, testTransactionId, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction")
                .hasMessageContaining(testTransactionId);

        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(transactionRepository, times(1)).findByIdAndBankAccount_AccountNumber(testTransactionId, testAccountNumber);
        verify(dtoMapper, never()).toTransactionResponse(any(Transaction.class));
    }

    @Test
    @DisplayName("getTransactionById - Unauthorized (user does not own account)")
    void testGetTransactionById_Unauthorized() {
        // Given
        User otherUser = new User();
        otherUser.setId(otherUserId);
        BankAccount otherUserAccount = new BankAccount();
        otherUserAccount.setAccountNumber(testAccountNumber);
        otherUserAccount.setUser(otherUser);

        when(bankAccountRepository.findByAccountNumber(testAccountNumber))
                .thenReturn(Optional.of(otherUserAccount));

        // When & Then - Should throw UnauthorizedException via AuthorizationHelper
        assertThatThrownBy(() -> transactionService.getTransactionById(
                testAccountNumber, testTransactionId, testUserId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("bank account");

        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(transactionRepository, never()).findByIdAndBankAccount_AccountNumber(anyString(), anyString());
    }

    @Test
    @DisplayName("getTransactionById - Verify service is called with correct parameters")
    void testGetTransactionById_VerifyServiceCalledWithCorrectParameters() {
        // Given
        when(bankAccountRepository.findByAccountNumber(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(transactionRepository.findByIdAndBankAccount_AccountNumber(testTransactionId, testAccountNumber))
                .thenReturn(Optional.of(testTransaction));
        when(dtoMapper.toTransactionResponse(testTransaction)).thenReturn(transactionResponse);

        // When
        transactionService.getTransactionById(testAccountNumber, testTransactionId, testUserId);

        // Then - Verify repository was called with correct transactionId and accountNumber
        verify(transactionRepository, times(1))
                .findByIdAndBankAccount_AccountNumber(eq(testTransactionId), eq(testAccountNumber));
    }

    @Test
    @DisplayName("createTransaction - Verify uses pessimistic lock")
    void testCreateTransaction_VerifyUsesPessimisticLock() {
        // Given
        when(bankAccountRepository.findByAccountNumberWithLock(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(idGenerator.generateTransactionId()).thenReturn(testTransactionId);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(dtoMapper.toTransactionResponse(any(Transaction.class))).thenReturn(transactionResponse);

        // When
        transactionService.createTransaction(testAccountNumber, createTransactionRequest, testUserId);

        // Then - Verify findByAccountNumberWithLock is used (not findByAccountNumber)
        verify(bankAccountRepository, times(1)).findByAccountNumberWithLock(testAccountNumber);
        verify(bankAccountRepository, never()).findByAccountNumber(anyString());
    }

    // --- Boundary Value Tests ---

    @Test
    @DisplayName("createTransaction - Minimum amount (0.01)")
    void testCreateTransaction_MinimumAmount() {
        // Given - Minimum valid amount (0.01)
        CreateTransactionRequest minRequest = new CreateTransactionRequest();
        minRequest.setAmount(0.01);
        minRequest.setCurrency(BankAccountResponse.Currency.GBP);
        minRequest.setType(CreateTransactionRequest.TransactionType.deposit);
        minRequest.setReference("Minimum Amount Test");

        TransactionResponse minResponse = new TransactionResponse();
        minResponse.setId(testTransactionId);
        minResponse.setAmount(0.01);
        minResponse.setCurrency(BankAccountResponse.Currency.GBP);
        minResponse.setType(CreateTransactionRequest.TransactionType.deposit);
        minResponse.setReference("Minimum Amount Test");
        minResponse.setUserId(testUserId);
        minResponse.setCreatedTimestamp(OffsetDateTime.now());

        when(bankAccountRepository.findByAccountNumberWithLock(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(idGenerator.generateTransactionId()).thenReturn(testTransactionId);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(dtoMapper.toTransactionResponse(any(Transaction.class))).thenReturn(minResponse);

        // When
        TransactionResponse response = transactionService.createTransaction(
                testAccountNumber, minRequest, testUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualTo(0.01);
        verify(bankAccountRepository, times(1)).findByAccountNumberWithLock(testAccountNumber);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("createTransaction - Maximum amount (10000.00)")
    void testCreateTransaction_MaximumAmount() {
        // Given - Maximum valid amount (10000.00)
        CreateTransactionRequest maxRequest = new CreateTransactionRequest();
        maxRequest.setAmount(10000.0);
        maxRequest.setCurrency(BankAccountResponse.Currency.GBP);
        maxRequest.setType(CreateTransactionRequest.TransactionType.deposit);
        maxRequest.setReference("Maximum Amount Test");

        TransactionResponse maxResponse = new TransactionResponse();
        maxResponse.setId(testTransactionId);
        maxResponse.setAmount(10000.0);
        maxResponse.setCurrency(BankAccountResponse.Currency.GBP);
        maxResponse.setType(CreateTransactionRequest.TransactionType.deposit);
        maxResponse.setReference("Maximum Amount Test");
        maxResponse.setUserId(testUserId);
        maxResponse.setCreatedTimestamp(OffsetDateTime.now());

        BigDecimal maxAmount = BigDecimal.valueOf(10000.0);
        BigDecimal initialBalance = BigDecimal.valueOf(1000.0);
        testBankAccount.setBalance(initialBalance);

        when(bankAccountRepository.findByAccountNumberWithLock(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(idGenerator.generateTransactionId()).thenReturn(testTransactionId);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(dtoMapper.toTransactionResponse(any(Transaction.class))).thenReturn(maxResponse);

        // When
        TransactionResponse response = transactionService.createTransaction(
                testAccountNumber, maxRequest, testUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualTo(10000.0);
        verify(bankAccountRepository, times(1)).save(argThat(account ->
                account.getBalance().compareTo(initialBalance.add(maxAmount)) == 0
        ));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("createTransaction - Large balance scenario (near maximum)")
    void testCreateTransaction_LargeBalanceScenario() {
        // Given - Account with very large balance
        BigDecimal largeBalance = BigDecimal.valueOf(999999.99);
        BigDecimal depositAmount = BigDecimal.valueOf(10000.0);
        testBankAccount.setBalance(largeBalance);

        TransactionResponse largeResponse = new TransactionResponse();
        largeResponse.setId(testTransactionId);
        largeResponse.setAmount(depositAmount.doubleValue());
        largeResponse.setCurrency(BankAccountResponse.Currency.GBP);
        largeResponse.setType(CreateTransactionRequest.TransactionType.deposit);
        largeResponse.setReference("Large Balance Test");
        largeResponse.setUserId(testUserId);
        largeResponse.setCreatedTimestamp(OffsetDateTime.now());

        createTransactionRequest.setAmount(depositAmount.doubleValue());

        when(bankAccountRepository.findByAccountNumberWithLock(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));
        when(idGenerator.generateTransactionId()).thenReturn(testTransactionId);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(dtoMapper.toTransactionResponse(any(Transaction.class))).thenReturn(largeResponse);

        // When
        TransactionResponse response = transactionService.createTransaction(
                testAccountNumber, createTransactionRequest, testUserId);

        // Then
        assertThat(response).isNotNull();
        verify(bankAccountRepository, times(1)).save(argThat(account ->
                account.getBalance().compareTo(largeBalance.add(depositAmount)) == 0
        ));
    }

    @Test
    @DisplayName("createTransaction - Zero balance withdrawal (should fail)")
    void testCreateTransaction_ZeroBalanceWithdrawal() {
        // Given - Account with zero balance, attempting withdrawal
        testBankAccount.setBalance(BigDecimal.ZERO);
        createTransactionRequest.setType(CreateTransactionRequest.TransactionType.withdrawal);
        createTransactionRequest.setAmount(0.01); // Minimum withdrawal amount

        when(bankAccountRepository.findByAccountNumberWithLock(testAccountNumber))
                .thenReturn(Optional.of(testBankAccount));

        // When & Then - Should throw InsufficientFundsException
        assertThatThrownBy(() -> transactionService.createTransaction(
                testAccountNumber, createTransactionRequest, testUserId))
                .isInstanceOf(InsufficientFundsException.class);

        verify(bankAccountRepository, times(1)).findByAccountNumberWithLock(testAccountNumber);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
