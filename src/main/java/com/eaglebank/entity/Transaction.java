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
@Table(name = "transactions")
public class Transaction {
    
    @Id
    @Column(name = "id", length = 50)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "account_number", nullable = false)
    private BankAccount bankAccount;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private BankAccount.Currency currency;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;
    
    @Column(name = "reference", length = 255)
    private String reference;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private OffsetDateTime createdTimestamp;
    
    public enum TransactionType {
        deposit, withdrawal
    }
    
    public Transaction() {
    }
    
    public Transaction(String id, BankAccount bankAccount, BigDecimal amount, 
                      BankAccount.Currency currency, TransactionType type, 
                      String reference, User user) {
        this.id = id;
        this.bankAccount = bankAccount;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.reference = reference;
        this.user = user;
        this.createdTimestamp = OffsetDateTime.now();
    }
    
    @jakarta.persistence.PrePersist
    protected void onCreate() {
        if (createdTimestamp == null) {
            createdTimestamp = OffsetDateTime.now();
        }
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public BankAccount getBankAccount() {
        return bankAccount;
    }
    
    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public BankAccount.Currency getCurrency() {
        return currency;
    }
    
    public void setCurrency(BankAccount.Currency currency) {
        this.currency = currency;
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public void setType(TransactionType type) {
        this.type = type;
    }
    
    public String getReference() {
        return reference;
    }
    
    public void setReference(String reference) {
        this.reference = reference;
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
}