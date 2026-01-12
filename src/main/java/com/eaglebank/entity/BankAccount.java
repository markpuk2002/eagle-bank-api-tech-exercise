package com.eaglebank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {
    
    @Id
    @Column(name = "account_number", length = 8)
    private String accountNumber;
    
    @Column(name = "sort_code", nullable = false, length = 8)
    private String sortCode;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;
    
    @Column(name = "balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal balance;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private OffsetDateTime createdTimestamp;
    
    @Column(name = "updated_timestamp", nullable = false)
    private OffsetDateTime updatedTimestamp;
    
    public enum AccountType {
        personal
    }
    
    public enum Currency {
        GBP
    }
    
    public BankAccount() {
    }
    
    public BankAccount(String accountNumber, String sortCode, String name, AccountType accountType, 
                      BigDecimal balance, Currency currency, User user) {
        this.accountNumber = accountNumber;
        this.sortCode = sortCode;
        this.name = name;
        this.accountType = accountType;
        this.balance = balance;
        this.currency = currency;
        this.user = user;
        this.createdTimestamp = OffsetDateTime.now();
        this.updatedTimestamp = OffsetDateTime.now();
    }
    
    @jakarta.persistence.PrePersist
    protected void onCreate() {
        if (createdTimestamp == null) {
            createdTimestamp = OffsetDateTime.now();
        }
        if (updatedTimestamp == null) {
            updatedTimestamp = OffsetDateTime.now();
        }
    }
    
    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        updatedTimestamp = OffsetDateTime.now();
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getSortCode() {
        return sortCode;
    }
    
    public void setSortCode(String sortCode) {
        this.sortCode = sortCode;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public AccountType getAccountType() {
        return accountType;
    }
    
    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public OffsetDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    public void setCreatedTimestamp(OffsetDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
    
    public OffsetDateTime getUpdatedTimestamp() {
        return updatedTimestamp;
    }
    
    public void setUpdatedTimestamp(OffsetDateTime updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
    }
}