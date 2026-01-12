package com.eaglebank.util;

import com.eaglebank.constants.ApiConstants;
import com.eaglebank.repository.BankAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Id Generator Unit Tests")
class IdGeneratorTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private IdGenerator idGenerator;

    // --- generateUserId Tests ---

    @Test
    @DisplayName("generateUserId - Success")
    void testGenerateUserId_Success() {
        // When
        String userId = idGenerator.generateUserId();

        // Then
        assertThat(userId).isNotNull();
        assertThat(userId).startsWith(ApiConstants.USER_ID_PREFIX);
        assertThat(userId).matches("^usr-[A-Za-z0-9]{12}$");
        assertThat(userId.length()).isEqualTo(16); // "usr-" + 12 characters
    }

    @Test
    @DisplayName("generateUserId - Verify format")
    void testGenerateUserId_VerifyFormat() {
        // When
        String userId = idGenerator.generateUserId();

        // Then
        assertThat(userId).matches("^usr-[A-Za-z0-9]{12}$");
    }

    @Test
    @DisplayName("generateUserId - Verify uniqueness (multiple calls)")
    void testGenerateUserId_VerifyUniqueness() {
        // When
        String userId1 = idGenerator.generateUserId();
        String userId2 = idGenerator.generateUserId();
        String userId3 = idGenerator.generateUserId();

        // Then - All should be different (very high probability with UUID)
        assertThat(userId1).isNotEqualTo(userId2);
        assertThat(userId2).isNotEqualTo(userId3);
        assertThat(userId1).isNotEqualTo(userId3);
    }

    // --- generateAccountNumber Tests ---

    @Test
    @DisplayName("generateAccountNumber - Success on first attempt")
    void testGenerateAccountNumber_SuccessFirstAttempt() {
        // Given
        when(bankAccountRepository.existsByAccountNumber(anyString())).thenReturn(false);

        // When
        String accountNumber = idGenerator.generateAccountNumber();

        // Then
        assertThat(accountNumber).isNotNull();
        assertThat(accountNumber).startsWith(ApiConstants.ACCOUNT_NUMBER_PREFIX);
        assertThat(accountNumber).matches("^01\\d{6}$");
        assertThat(accountNumber.length()).isEqualTo(8); // "01" + 6 digits
        verify(bankAccountRepository, times(1)).existsByAccountNumber(anyString());
    }

    @Test
    @DisplayName("generateAccountNumber - Success after retry")
    void testGenerateAccountNumber_SuccessAfterRetry() {
        // Given - First attempt returns existing, second attempt is unique
        when(bankAccountRepository.existsByAccountNumber(anyString()))
                .thenReturn(true)  // First call - exists
                .thenReturn(false); // Second call - unique

        // When
        String accountNumber = idGenerator.generateAccountNumber();

        // Then
        assertThat(accountNumber).isNotNull();
        assertThat(accountNumber).matches("^01\\d{6}$");
        verify(bankAccountRepository, atLeast(2)).existsByAccountNumber(anyString());
    }

    @Test
    @DisplayName("generateAccountNumber - Verify format")
    void testGenerateAccountNumber_VerifyFormat() {
        // Given
        when(bankAccountRepository.existsByAccountNumber(anyString())).thenReturn(false);

        // When
        String accountNumber = idGenerator.generateAccountNumber();

        // Then
        assertThat(accountNumber).matches("^01\\d{6}$");
    }

    @Test
    @DisplayName("generateAccountNumber - Verify range of random digits")
    void testGenerateAccountNumber_VerifyRange() {
        // Given
        when(bankAccountRepository.existsByAccountNumber(anyString())).thenReturn(false);

        // When - Generate multiple account numbers to verify range
        for (int i = 0; i < 100; i++) {
            String accountNumber = idGenerator.generateAccountNumber();
            String digits = accountNumber.substring(2); // Remove "01" prefix
            int numericValue = Integer.parseInt(digits);

            // Then
            assertThat(numericValue).isGreaterThanOrEqualTo(ApiConstants.ACCOUNT_NUMBER_RANDOM_DIGITS_MIN);
            assertThat(numericValue).isLessThanOrEqualTo(ApiConstants.ACCOUNT_NUMBER_RANDOM_DIGITS_MAX);
        }
    }

    @Test
    @DisplayName("generateAccountNumber - Max attempts exceeded")
    void testGenerateAccountNumber_MaxAttemptsExceeded() {
        // Given - Always returns true (account number exists)
        when(bankAccountRepository.existsByAccountNumber(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> idGenerator.generateAccountNumber())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to generate unique account number")
                .hasMessageContaining(String.valueOf(ApiConstants.MAX_ACCOUNT_GENERATION_ATTEMPTS));

        // Verify repository was called max attempts times
        verify(bankAccountRepository, times(ApiConstants.MAX_ACCOUNT_GENERATION_ATTEMPTS))
                .existsByAccountNumber(anyString());
    }

    // --- generateTransactionId Tests ---

    @Test
    @DisplayName("generateTransactionId - Success")
    void testGenerateTransactionId_Success() {
        // When
        String transactionId = idGenerator.generateTransactionId();

        // Then
        assertThat(transactionId).isNotNull();
        assertThat(transactionId).startsWith(ApiConstants.TRANSACTION_ID_PREFIX);
        assertThat(transactionId).matches("^tan-[A-Za-z0-9]{12}$");
        assertThat(transactionId.length()).isEqualTo(16); // "tan-" + 12 characters
    }

    @Test
    @DisplayName("generateTransactionId - Verify format")
    void testGenerateTransactionId_VerifyFormat() {
        // When
        String transactionId = idGenerator.generateTransactionId();

        // Then
        assertThat(transactionId).matches("^tan-[A-Za-z0-9]{12}$");
    }

    @Test
    @DisplayName("generateTransactionId - Verify uniqueness (multiple calls)")
    void testGenerateTransactionId_VerifyUniqueness() {
        // When
        String transactionId1 = idGenerator.generateTransactionId();
        String transactionId2 = idGenerator.generateTransactionId();
        String transactionId3 = idGenerator.generateTransactionId();

        // Then - All should be different (very high probability with UUID)
        assertThat(transactionId1).isNotEqualTo(transactionId2);
        assertThat(transactionId2).isNotEqualTo(transactionId3);
        assertThat(transactionId1).isNotEqualTo(transactionId3);
    }
}
