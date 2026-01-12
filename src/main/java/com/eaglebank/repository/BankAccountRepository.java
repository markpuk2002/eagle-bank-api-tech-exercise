package com.eaglebank.repository;

import com.eaglebank.entity.BankAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, String> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ba FROM BankAccount ba WHERE ba.accountNumber = :accountNumber")
    Optional<BankAccount> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);
    
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    
    List<BankAccount> findByUser_Id(String userId);
    
    boolean existsByAccountNumber(String accountNumber);
    
    boolean existsByUser_Id(String userId);
}