package com.eaglebank.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when a withdrawal transaction cannot be processed due to insufficient funds.
 * Contains details about the account, requested amount, and current balance.
 */
public class InsufficientFundsException extends RuntimeException {
    
    private final String accountNumber;
    private final BigDecimal requestedAmount;
    private final BigDecimal currentBalance;
    
    /**
     * Creates a new InsufficientFundsException.
     *
     * @param accountNumber The account number with insufficient funds
     * @param requestedAmount The amount requested for withdrawal
     * @param currentBalance The current balance of the account
     */
    public InsufficientFundsException(String accountNumber, BigDecimal requestedAmount, BigDecimal currentBalance) {
        super(String.format("Insufficient funds to process transaction. Account: %s, Requested: %s, Available: %s",
            accountNumber, requestedAmount, currentBalance));
        this.accountNumber = accountNumber;
        this.requestedAmount = requestedAmount;
        this.currentBalance = currentBalance;
    }
    
    /**
     * Gets the account number with insufficient funds.
     *
     * @return The account number
     */
    public String getAccountNumber() {
        return accountNumber;
    }
    
    /**
     * Gets the requested withdrawal amount.
     *
     * @return The requested amount
     */
    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
    
    /**
     * Gets the current balance of the account.
     *
     * @return The current balance
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
}
