package com.eaglebank.repository;

import com.eaglebank.entity.BankAccount;
import com.eaglebank.entity.Transaction;
import com.eaglebank.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Transaction Repository Integration Tests")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private BankAccount testAccount;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId("usr-123456789012");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setCreatedTimestamp(OffsetDateTime.now());
        testUser.setUpdatedTimestamp(OffsetDateTime.now());
        userRepository.save(testUser);

        // Create test account
        testAccount = new BankAccount();
        testAccount.setAccountNumber("01000001");
        testAccount.setSortCode("01-00-00");
        testAccount.setName("Test Account");
        testAccount.setAccountType(BankAccount.AccountType.personal);
        testAccount.setBalance(BigDecimal.valueOf(1000.0));
        testAccount.setCurrency(BankAccount.Currency.GBP);
        testAccount.setUser(testUser);
        testAccount.setCreatedTimestamp(OffsetDateTime.now());
        testAccount.setUpdatedTimestamp(OffsetDateTime.now());
        bankAccountRepository.save(testAccount);

        // Create test transaction
        testTransaction = new Transaction();
        testTransaction.setId("tan-123456789012");
        testTransaction.setBankAccount(testAccount);
        testTransaction.setAmount(BigDecimal.valueOf(100.0));
        testTransaction.setCurrency(BankAccount.Currency.GBP);
        testTransaction.setType(Transaction.TransactionType.deposit);
        testTransaction.setReference("Test Transaction");
        testTransaction.setUser(testUser);
        testTransaction.setCreatedTimestamp(OffsetDateTime.now());
        transactionRepository.save(testTransaction);
    }

    @Test
    @DisplayName("findById - Success")
    void testFindById_Success() {
        // When
        Optional<Transaction> result = transactionRepository.findById("tan-123456789012");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("tan-123456789012");
        assertThat(result.get().getAmount()).isEqualTo(BigDecimal.valueOf(100.0));
        assertThat(result.get().getType()).isEqualTo(Transaction.TransactionType.deposit);
        assertThat(result.get().getBankAccount().getAccountNumber()).isEqualTo("01000001");
        assertThat(result.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("findById - Not found")
    void testFindById_NotFound() {
        // When
        Optional<Transaction> result = transactionRepository.findById("tan-999999999999");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByBankAccount_AccountNumber - Success with multiple transactions")
    void testFindByBankAccount_AccountNumber_Success() {
        // Given - Create another transaction for the same account
        Transaction secondTransaction = new Transaction();
        secondTransaction.setId("tan-987654321098");
        secondTransaction.setBankAccount(testAccount);
        secondTransaction.setAmount(BigDecimal.valueOf(200.0));
        secondTransaction.setCurrency(BankAccount.Currency.GBP);
        secondTransaction.setType(Transaction.TransactionType.withdrawal);
        secondTransaction.setReference("Second Transaction");
        secondTransaction.setUser(testUser);
        secondTransaction.setCreatedTimestamp(OffsetDateTime.now());
        transactionRepository.save(secondTransaction);

        // When
        List<Transaction> transactions = transactionRepository.findByBankAccount_AccountNumber("01000001");

        // Then
        assertThat(transactions).hasSize(2);
        assertThat(transactions).extracting(Transaction::getId)
                .containsExactlyInAnyOrder("tan-123456789012", "tan-987654321098");
    }

    @Test
    @DisplayName("findByBankAccount_AccountNumber - Empty list for account with no transactions")
    void testFindByBankAccount_AccountNumber_EmptyList() {
        // Given - Create an account with no transactions
        BankAccount newAccount = new BankAccount();
        newAccount.setAccountNumber("01000002");
        newAccount.setSortCode("01-00-00");
        newAccount.setName("New Account");
        newAccount.setAccountType(BankAccount.AccountType.personal);
        newAccount.setBalance(BigDecimal.ZERO);
        newAccount.setCurrency(BankAccount.Currency.GBP);
        newAccount.setUser(testUser);
        newAccount.setCreatedTimestamp(OffsetDateTime.now());
        newAccount.setUpdatedTimestamp(OffsetDateTime.now());
        bankAccountRepository.save(newAccount);

        // When
        List<Transaction> transactions = transactionRepository.findByBankAccount_AccountNumber("01000002");

        // Then
        assertThat(transactions).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndBankAccount_AccountNumber - Success")
    void testFindByIdAndBankAccount_AccountNumber_Success() {
        // When
        Optional<Transaction> result = transactionRepository.findByIdAndBankAccount_AccountNumber(
                "tan-123456789012", "01000001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("tan-123456789012");
        assertThat(result.get().getBankAccount().getAccountNumber()).isEqualTo("01000001");
    }

    @Test
    @DisplayName("findByIdAndBankAccount_AccountNumber - Transaction ID not found")
    void testFindByIdAndBankAccount_AccountNumber_TransactionNotFound() {
        // When
        Optional<Transaction> result = transactionRepository.findByIdAndBankAccount_AccountNumber(
                "tan-999999999999", "01000001");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndBankAccount_AccountNumber - Account number mismatch")
    void testFindByIdAndBankAccount_AccountNumber_AccountMismatch() {
        // Given - Create another account
        BankAccount otherAccount = new BankAccount();
        otherAccount.setAccountNumber("01000002");
        otherAccount.setSortCode("01-00-00");
        otherAccount.setName("Other Account");
        otherAccount.setAccountType(BankAccount.AccountType.personal);
        otherAccount.setBalance(BigDecimal.ZERO);
        otherAccount.setCurrency(BankAccount.Currency.GBP);
        otherAccount.setUser(testUser);
        otherAccount.setCreatedTimestamp(OffsetDateTime.now());
        otherAccount.setUpdatedTimestamp(OffsetDateTime.now());
        bankAccountRepository.save(otherAccount);

        // When - Try to find transaction with wrong account number
        Optional<Transaction> result = transactionRepository.findByIdAndBankAccount_AccountNumber(
                "tan-123456789012", "01000002");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save - Create new transaction")
    void testSave_CreateNewTransaction() {
        // Given
        Transaction newTransaction = new Transaction();
        newTransaction.setId("tan-111111111111");
        newTransaction.setBankAccount(testAccount);
        newTransaction.setAmount(BigDecimal.valueOf(300.0));
        newTransaction.setCurrency(BankAccount.Currency.GBP);
        newTransaction.setType(Transaction.TransactionType.deposit);
        newTransaction.setReference("New Transaction");
        newTransaction.setUser(testUser);
        newTransaction.setCreatedTimestamp(OffsetDateTime.now());

        // When
        Transaction saved = transactionRepository.save(newTransaction);

        // Then
        assertThat(saved.getId()).isEqualTo("tan-111111111111");
        assertThat(saved.getAmount()).isEqualTo(BigDecimal.valueOf(300.0));
        
        // Verify it can be retrieved
        Optional<Transaction> retrieved = transactionRepository.findById("tan-111111111111");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getAmount()).isEqualTo(BigDecimal.valueOf(300.0));
    }
}
