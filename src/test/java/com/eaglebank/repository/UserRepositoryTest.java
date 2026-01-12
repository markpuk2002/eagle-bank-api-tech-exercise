package com.eaglebank.repository;

import com.eaglebank.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("User Repository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

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
    }

    @Test
    @DisplayName("findById - Success")
    void testFindById_Success() {
        // When
        Optional<User> result = userRepository.findById("usr-123456789012");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("usr-123456789012");
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("findById - Not found")
    void testFindById_NotFound() {
        // When
        Optional<User> result = userRepository.findById("usr-999999999999");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUsername - Success")
    void testFindByUsername_Success() {
        // When
        Optional<User> result = userRepository.findByUsername("testuser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("findByUsername - Not found")
    void testFindByUsername_NotFound() {
        // When
        Optional<User> result = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail - Email exists")
    void testExistsByEmail_Exists() {
        // When
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail - Email does not exist")
    void testExistsByEmail_NotExists() {
        // When
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByUsername - Username exists")
    void testExistsByUsername_Exists() {
        // When
        boolean exists = userRepository.existsByUsername("testuser");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByUsername - Username does not exist")
    void testExistsByUsername_NotExists() {
        // When
        boolean exists = userRepository.existsByUsername("nonexistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("save - Create new user")
    void testSave_CreateNewUser() {
        // Given
        User newUser = new User();
        newUser.setId("usr-999999999999");
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");
        newUser.setPassword("encodedPassword");
        newUser.setCreatedTimestamp(OffsetDateTime.now());
        newUser.setUpdatedTimestamp(OffsetDateTime.now());

        // When
        User saved = userRepository.save(newUser);

        // Then
        assertThat(saved.getId()).isEqualTo("usr-999999999999");
        assertThat(saved.getUsername()).isEqualTo("newuser");
        assertThat(saved.getEmail()).isEqualTo("newuser@example.com");
        
        // Verify it can be retrieved
        Optional<User> retrieved = userRepository.findById("usr-999999999999");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getUsername()).isEqualTo("newuser");
    }

    @Test
    @DisplayName("save - Update existing user")
    void testSave_UpdateExistingUser() {
        // Given
        testUser.setEmail("updated@example.com");
        testUser.setName("Updated Name");

        // When
        User updated = userRepository.save(testUser);

        // Then
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
        assertThat(updated.getName()).isEqualTo("Updated Name");
        
        // Verify update persisted
        Optional<User> retrieved = userRepository.findById("usr-123456789012");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getEmail()).isEqualTo("updated@example.com");
        assertThat(retrieved.get().getName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("existsByEmail - Case sensitivity")
    void testExistsByEmail_CaseSensitivity() {
        // When - Email should be case-sensitive
        boolean exists = userRepository.existsByEmail("TEST@EXAMPLE.COM");

        // Then - Should not find (case-sensitive)
        // Note: MySQL comparison depends on collation, H2 is case-sensitive by default
        assertThat(exists).isFalse();
    }
}
