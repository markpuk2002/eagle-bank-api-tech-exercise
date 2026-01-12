package com.eaglebank.repository;

import com.eaglebank.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    Optional<User> findById(String id);

    Optional<User> findByUsername(String username);
    
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}