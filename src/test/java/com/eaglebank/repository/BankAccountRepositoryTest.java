package com.eaglebank.repository;

import com.eaglebank.entity.BankAccount;
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
@DisplayName("Bank Account Repository Integration Tests")
class BankAccountRepositoryTest {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private BankAccount testAccount;

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
    }

    @Test
    @DisplayName("findByAccountNumber - Success")
    void testFindByAccountNumber_Success() {
        // When
        Optional<BankAccount> result = bankAccountRepository.findByAccountNumber("01000001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountNumber()).isEqualTo("01000001");
        assertThat(result.get().getName()).isEqualTo("Test Account");
        assertThat(result.get().getBalance()).isEqualTo(BigDecimal.valueOf(1000.0));
        assertThat(result.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("findByAccountNumber - Not found")
    void testFindByAccountNumber_NotFound() {
        // When
        Optional<BankAccount> result = bankAccountRepository.findByAccountNumber("01999999");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByAccountNumberWithLock - Success")
    void testFindByAccountNumberWithLock_Success() {
        // When
        Optional<BankAccount> result = bankAccountRepository.findByAccountNumberWithLock("01000001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountNumber()).isEqualTo("01000001");
        assertThat(result.get().getName()).isEqualTo("Test Account");
        assertThat(result.get().getBalance()).isEqualTo(BigDecimal.valueOf(1000.0));
        assertThat(result.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("findByAccountNumberWithLock - Not found")
    void testFindByAccountNumberWithLock_NotFound() {
        // When
        Optional<BankAccount> result = bankAccountRepository.findByAccountNumberWithLock("01999999");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUser_Id - Success with multiple accounts")
    void testFindByUser_Id_Success() {
        // Given - Create another account for the same user
        BankAccount secondAccount = new BankAccount();
        secondAccount.setAccountNumber("01000002");
        secondAccount.setSortCode("01-00-00");
        secondAccount.setName("Second Account");
        secondAccount.setAccountType(BankAccount.AccountType.personal);
        secondAccount.setBalance(BigDecimal.valueOf(500.0));
        secondAccount.setCurrency(BankAccount.Currency.GBP);
        secondAccount.setUser(testUser);
        secondAccount.setCreatedTimestamp(OffsetDateTime.now());
        secondAccount.setUpdatedTimestamp(OffsetDateTime.now());
        bankAccountRepository.save(secondAccount);

        // When
        List<BankAccount> accounts = bankAccountRepository.findByUser_Id(testUser.getId());

        // Then
        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(BankAccount::getAccountNumber)
                .containsExactlyInAnyOrder("01000001", "01000002");
    }

    @Test
    @DisplayName("findByUser_Id - Empty list for user with no accounts")
    void testFindByUser_Id_EmptyList() {
        // Given - Create a user with no accounts
        User newUser = new User();
        newUser.setId("usr-999999999999");
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");
        newUser.setPassword("encodedPassword");
        newUser.setCreatedTimestamp(OffsetDateTime.now());
        newUser.setUpdatedTimestamp(OffsetDateTime.now());
        userRepository.save(newUser);

        // When
        List<BankAccount> accounts = bankAccountRepository.findByUser_Id(newUser.getId());

        // Then
        assertThat(accounts).isEmpty();
    }

    @Test
    @DisplayName("existsByAccountNumber - Account exists")
    void testExistsByAccountNumber_Exists() {
        // When
        boolean exists = bankAccountRepository.existsByAccountNumber("01000001");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByAccountNumber - Account does not exist")
    void testExistsByAccountNumber_NotExists() {
        // When
        boolean exists = bankAccountRepository.existsByAccountNumber("01999999");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByUser_Id - User has accounts")
    void testExistsByUser_Id_HasAccounts() {
        // When
        boolean exists = bankAccountRepository.existsByUser_Id(testUser.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByUser_Id - User has no accounts")
    void testExistsByUser_Id_NoAccounts() {
        // Given - Create a user with no accounts
        User newUser = new User();
        newUser.setId("usr-999999999999");
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");
        newUser.setPassword("encodedPassword");
        newUser.setCreatedTimestamp(OffsetDateTime.now());
        newUser.setUpdatedTimestamp(OffsetDateTime.now());
        userRepository.save(newUser);

        // When
        boolean exists = bankAccountRepository.existsByUser_Id(newUser.getId());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("save - Create new account")
    void testSave_CreateNewAccount() {
        // Given
        BankAccount newAccount = new BankAccount();
        newAccount.setAccountNumber("01000003");
        newAccount.setSortCode("01-00-00");
        newAccount.setName("New Account");
        newAccount.setAccountType(BankAccount.AccountType.personal);
        newAccount.setBalance(BigDecimal.ZERO);
        newAccount.setCurrency(BankAccount.Currency.GBP);
        newAccount.setUser(testUser);
        newAccount.setCreatedTimestamp(OffsetDateTime.now());
        newAccount.setUpdatedTimestamp(OffsetDateTime.now());

        // When
        BankAccount saved = bankAccountRepository.save(newAccount);

        // Then
        assertThat(saved.getAccountNumber()).isEqualTo("01000003");
        assertThat(saved.getName()).isEqualTo("New Account");
        
        // Verify it can be retrieved
        Optional<BankAccount> retrieved = bankAccountRepository.findByAccountNumber("01000003");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("New Account");
    }

    @Test
    @DisplayName("save - Update existing account")
    void testSave_UpdateExistingAccount() {
        // Given
        testAccount.setName("Updated Account Name");
        testAccount.setBalance(BigDecimal.valueOf(2000.0));

        // When
        BankAccount updated = bankAccountRepository.save(testAccount);

        // Then
        assertThat(updated.getName()).isEqualTo("Updated Account Name");
        assertThat(updated.getBalance()).isEqualTo(BigDecimal.valueOf(2000.0));
        
        // Verify update persisted
        Optional<BankAccount> retrieved = bankAccountRepository.findByAccountNumber("01000001");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Updated Account Name");
        assertThat(retrieved.get().getBalance()).isEqualTo(BigDecimal.valueOf(2000.0));
    }

    @Test
    @DisplayName("deleteById - Delete account")
    void testDeleteById_Success() {
        // When
        bankAccountRepository.deleteById("01000001");

        // Then
        Optional<BankAccount> deleted = bankAccountRepository.findByAccountNumber("01000001");
        assertThat(deleted).isEmpty();
        
        boolean exists = bankAccountRepository.existsByAccountNumber("01000001");
        assertThat(exists).isFalse();
    }
}
