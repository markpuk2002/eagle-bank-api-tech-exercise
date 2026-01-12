package com.eaglebank.util;

import com.eaglebank.constants.ApiConstants;
import com.eaglebank.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class IdGenerator {

    private static final Logger log = LoggerFactory.getLogger(IdGenerator.class);

    private final BankAccountRepository bankAccountRepository;

    /**
     * Generates a unique user ID.
     * Format: usr-{12 alphanumeric characters}
     * 
     * @return A unique user ID
     */
    public String generateUserId() {
        return ApiConstants.USER_ID_PREFIX +
            UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * Generates a unique account number.
     * Format: 01{6 digits}
     * Includes retry logic to ensure uniqueness.
     * 
     * @return A unique account number
     * @throws IllegalStateException if unable to generate unique account number after max attempts
     */
    public String generateAccountNumber() {
        String accountNumber;
        int attempts = 0;
        
        do {
            if (++attempts > ApiConstants.MAX_ACCOUNT_GENERATION_ATTEMPTS) {
                throw new IllegalStateException(
                    String.format(
                        "Unable to generate unique account number after %d attempts. " +
                        "Please contact system administrator.",
                        ApiConstants.MAX_ACCOUNT_GENERATION_ATTEMPTS
                    )
                );
            }
            // Use ThreadLocalRandom for thread-safety
            int randomDigits = ApiConstants.ACCOUNT_NUMBER_RANDOM_DIGITS_MIN + 
                ThreadLocalRandom.current().nextInt(
                    ApiConstants.ACCOUNT_NUMBER_RANDOM_DIGITS_MAX - ApiConstants.ACCOUNT_NUMBER_RANDOM_DIGITS_MIN + 1
                );
            accountNumber = ApiConstants.ACCOUNT_NUMBER_PREFIX + randomDigits;
        } while (bankAccountRepository.existsByAccountNumber(accountNumber));
        
        if (attempts > 1) {
            log.debug("Account number generated after {} attempts", attempts);
        }
        
        return accountNumber;
    }

    /**
     * Generates a unique transaction ID.
     * Format: tan-{12 alphanumeric characters}
     * 
     * @return A unique transaction ID
     */
    public String generateTransactionId() {
        return ApiConstants.TRANSACTION_ID_PREFIX +
            UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
