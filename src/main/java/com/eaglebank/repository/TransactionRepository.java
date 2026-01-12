package com.eaglebank.repository;

import com.eaglebank.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    
    Optional<Transaction> findById(String id);
    
    List<Transaction> findByBankAccount_AccountNumber(String accountNumber);
    
    Optional<Transaction> findByIdAndBankAccount_AccountNumber(String id, String accountNumber);
}