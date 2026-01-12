package com.eaglebank.service;

import com.eaglebank.dto.request.CreateTransactionRequest;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.entity.BankAccount;
import com.eaglebank.entity.User;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.TransactionRepository;
import com.eaglebank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Transaction Service Concurrent Tests")
class TransactionServiceConcurrentTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User testUser;
    private BankAccount testAccount;
    private String testUserId;
    private String testAccountNumber;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create test user
        testUserId = "usr-concurrent123";
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername("concurrentuser");
        testUser.setEmail("concurrent@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setCreatedTimestamp(OffsetDateTime.now());
        testUser.setUpdatedTimestamp(OffsetDateTime.now());
        userRepository.save(testUser);

        // Create test account with sufficient balance
        testAccountNumber = "01000010";
        testAccount = new BankAccount();
        testAccount.setAccountNumber(testAccountNumber);
        testAccount.setSortCode("01-00-00");
        testAccount.setName("Concurrent Test Account");
        testAccount.setAccountType(BankAccount.AccountType.personal);
        testAccount.setBalance(BigDecimal.valueOf(10000.0)); // High balance for concurrent tests
        testAccount.setCurrency(BankAccount.Currency.GBP);
        testAccount.setUser(testUser);
        testAccount.setCreatedTimestamp(OffsetDateTime.now());
        testAccount.setUpdatedTimestamp(OffsetDateTime.now());
        bankAccountRepository.save(testAccount);
    }

    @Test
    @DisplayName("createTransaction - Concurrent deposits maintain balance integrity")
    void testCreateTransaction_ConcurrentDeposits() throws Exception {
        // Given - Account with initial balance
        BigDecimal initialBalance = testAccount.getBalance();
        int numberOfThreads = 10;
        BigDecimal depositAmount = BigDecimal.valueOf(100.0);
        BigDecimal expectedFinalBalance = initialBalance.add(depositAmount.multiply(BigDecimal.valueOf(numberOfThreads)));
        
        // Count existing transactions before test
        long initialTransactionCount = transactionRepository.findByBankAccount_AccountNumber(testAccountNumber).size();

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // When - Execute concurrent deposits
        for (int i = 0; i < numberOfThreads; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    CreateTransactionRequest request = new CreateTransactionRequest();
                    request.setAmount(depositAmount.doubleValue());
                    request.setCurrency(BankAccountResponse.Currency.GBP);
                    request.setType(CreateTransactionRequest.TransactionType.deposit);
                    request.setReference("Concurrent Deposit");

                    transactionService.createTransaction(testAccountNumber, request, testUserId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all transactions to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Then - Verify all transactions succeeded
        assertThat(successCount.get()).isEqualTo(numberOfThreads);
        assertThat(errorCount.get()).isEqualTo(0);

        // Verify final balance is correct (using pessimistic locking prevents race conditions)
        BankAccount updatedAccount = bankAccountRepository.findByAccountNumber(testAccountNumber).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(expectedFinalBalance);

        // Verify all transactions were created (check that the correct number were added)
        long finalTransactionCount = transactionRepository.findByBankAccount_AccountNumber(testAccountNumber).size();
        assertThat(finalTransactionCount - initialTransactionCount).isEqualTo(numberOfThreads);
    }

    @Test
    @DisplayName("createTransaction - Concurrent withdrawals maintain balance integrity")
    void testCreateTransaction_ConcurrentWithdrawals() throws Exception {
        // Given - Account with sufficient balance
        BigDecimal initialBalance = BigDecimal.valueOf(5000.0);
        testAccount.setBalance(initialBalance);
        bankAccountRepository.save(testAccount);
        
        // Count existing transactions before test
        long initialTransactionCount = transactionRepository.findByBankAccount_AccountNumber(testAccountNumber).size();

        int numberOfThreads = 5;
        BigDecimal withdrawalAmount = BigDecimal.valueOf(100.0);
        BigDecimal expectedFinalBalance = initialBalance.subtract(withdrawalAmount.multiply(BigDecimal.valueOf(numberOfThreads)));

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // When - Execute concurrent withdrawals
        for (int i = 0; i < numberOfThreads; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    CreateTransactionRequest request = new CreateTransactionRequest();
                    request.setAmount(withdrawalAmount.doubleValue());
                    request.setCurrency(BankAccountResponse.Currency.GBP);
                    request.setType(CreateTransactionRequest.TransactionType.withdrawal);
                    request.setReference("Concurrent Withdrawal");

                    transactionService.createTransaction(testAccountNumber, request, testUserId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all transactions to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Then - Verify all transactions succeeded
        assertThat(successCount.get()).isEqualTo(numberOfThreads);
        assertThat(errorCount.get()).isEqualTo(0);

        // Verify final balance is correct (pessimistic locking prevents race conditions)
        BankAccount updatedAccount = bankAccountRepository.findByAccountNumber(testAccountNumber).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(expectedFinalBalance);

        // Verify all transactions were created (check that the correct number were added)
        long finalTransactionCount = transactionRepository.findByBankAccount_AccountNumber(testAccountNumber).size();
        assertThat(finalTransactionCount - initialTransactionCount).isEqualTo(numberOfThreads);
    }

    @Test
    @DisplayName("createTransaction - Concurrent mixed transactions maintain balance integrity")
    void testCreateTransaction_ConcurrentMixedTransactions() throws Exception {
        // Given - Account with initial balance
        BigDecimal initialBalance = BigDecimal.valueOf(3000.0);
        testAccount.setBalance(initialBalance);
        bankAccountRepository.save(testAccount);
        
        // Count existing transactions before test
        long initialTransactionCount = transactionRepository.findByBankAccount_AccountNumber(testAccountNumber).size();

        int numberOfDeposits = 5;
        int numberOfWithdrawals = 3;
        BigDecimal transactionAmount = BigDecimal.valueOf(100.0);
        BigDecimal expectedFinalBalance = initialBalance
                .add(transactionAmount.multiply(BigDecimal.valueOf(numberOfDeposits)))
                .subtract(transactionAmount.multiply(BigDecimal.valueOf(numberOfWithdrawals)));

        ExecutorService executor = Executors.newFixedThreadPool(numberOfDeposits + numberOfWithdrawals);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // When - Execute concurrent deposits
        for (int i = 0; i < numberOfDeposits; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    CreateTransactionRequest request = new CreateTransactionRequest();
                    request.setAmount(transactionAmount.doubleValue());
                    request.setCurrency(BankAccountResponse.Currency.GBP);
                    request.setType(CreateTransactionRequest.TransactionType.deposit);
                    request.setReference("Concurrent Deposit");

                    transactionService.createTransaction(testAccountNumber, request, testUserId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            }, executor);
            futures.add(future);
        }

        // Execute concurrent withdrawals
        for (int i = 0; i < numberOfWithdrawals; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    CreateTransactionRequest request = new CreateTransactionRequest();
                    request.setAmount(transactionAmount.doubleValue());
                    request.setCurrency(BankAccountResponse.Currency.GBP);
                    request.setType(CreateTransactionRequest.TransactionType.withdrawal);
                    request.setReference("Concurrent Withdrawal");

                    transactionService.createTransaction(testAccountNumber, request, testUserId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all transactions to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Then - Verify all transactions succeeded
        assertThat(successCount.get()).isEqualTo(numberOfDeposits + numberOfWithdrawals);
        assertThat(errorCount.get()).isEqualTo(0);

        // Verify final balance is correct (pessimistic locking prevents race conditions)
        BankAccount updatedAccount = bankAccountRepository.findByAccountNumber(testAccountNumber).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(expectedFinalBalance);

        // Verify all transactions were created (check that the correct number were added)
        long finalTransactionCount = transactionRepository.findByBankAccount_AccountNumber(testAccountNumber).size();
        assertThat(finalTransactionCount - initialTransactionCount).isEqualTo(numberOfDeposits + numberOfWithdrawals);
    }
}
