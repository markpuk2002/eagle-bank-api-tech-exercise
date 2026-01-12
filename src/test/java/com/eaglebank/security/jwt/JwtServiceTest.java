package com.eaglebank.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Service Unit Tests")
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private String testSecretKey;
    private long testExpiration;
    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        // Generate a test secret key (32 bytes, Base64 encoded) - using same approach as production
        // Use a fixed test key for consistent testing
        testSecretKey = "dGVzdC1zZWNyZXQta2V5LTMyLWJ5dGVzLWxvbmchISE="; // "test-secret-key-32-bytes-long!!"
        testExpiration = TimeUnit.HOURS.toMillis(24); // 24 hours

        // Set private fields using ReflectionTestUtils
        ReflectionTestUtils.setField(jwtService, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(jwtService, "expiration", testExpiration);

        // Create test UserDetails
        testUserDetails = User.builder()
                .username("testuser")
                .password("password")
                .build();
    }

    // --- generateToken Tests ---

    @Test
    @DisplayName("generateToken - Success")
    void testGenerateToken_Success() {
        // Given
        String username = "testuser";
        String userId = "usr-abcdefghijkl";

        // When
        String token = jwtService.generateToken(username, userId);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generateToken - Verify token contains username")
    void testGenerateToken_VerifyUsername() {
        // Given
        String username = "testuser";
        String userId = "usr-abcdefghijkl";

        // When
        String token = jwtService.generateToken(username, userId);
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("generateToken - Verify token contains userId")
    void testGenerateToken_VerifyUserId() {
        // Given
        String username = "testuser";
        String userId = "usr-abcdefghijkl";

        // When
        String token = jwtService.generateToken(username, userId);
        String extractedUserId = jwtService.extractUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("generateToken - Verify token is valid")
    void testGenerateToken_VerifyTokenValid() {
        // Given
        String username = "testuser";
        String userId = "usr-abcdefghijkl";

        // When
        String token = jwtService.generateToken(username, userId);
        boolean isValid = jwtService.isTokenValid(token, testUserDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("generateToken - Generate multiple tokens successfully")
    void testGenerateToken_GenerateMultipleTokens() {
        // Given
        String username = "testuser";
        String userId = "usr-abcdefghijkl";

        // When
        String token1 = jwtService.generateToken(username, userId);
        String token2 = jwtService.generateToken(username, userId);

        // Then - Both tokens should be valid (they may be identical if generated in same millisecond)
        assertThat(token1).isNotNull();
        assertThat(token1).isNotBlank();
        assertThat(token2).isNotNull();
        assertThat(token2).isNotBlank();
        assertThat(jwtService.extractUsername(token1)).isEqualTo(username);
        assertThat(jwtService.extractUsername(token2)).isEqualTo(username);
        assertThat(jwtService.isTokenValid(token1, testUserDetails)).isTrue();
        assertThat(jwtService.isTokenValid(token2, testUserDetails)).isTrue();
    }

    // --- extractUsername Tests ---

    @Test
    @DisplayName("extractUsername - Success")
    void testExtractUsername_Success() {
        // Given
        String username = "testuser";
        String userId = "usr-abcdefghijkl";
        String token = jwtService.generateToken(username, userId);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("extractUsername - Verify with different usernames")
    void testExtractUsername_DifferentUsernames() {
        // Given
        String[] usernames = {"user1", "user2", "testuser", "admin"};

        for (String username : usernames) {
            String token = jwtService.generateToken(username, "usr-test");
            String extractedUsername = jwtService.extractUsername(token);
            assertThat(extractedUsername).isEqualTo(username);
        }
    }

    // --- extractUserId Tests ---

    @Test
    @DisplayName("extractUserId - Success")
    void testExtractUserId_Success() {
        // Given
        String username = "testuser";
        String userId = "usr-abcdefghijkl";
        String token = jwtService.generateToken(username, userId);

        // When
        String extractedUserId = jwtService.extractUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("extractUserId - Verify with different userIds")
    void testExtractUserId_DifferentUserIds() {
        // Given
        String[] userIds = {"usr-123456789012", "usr-abcdefghijkl", "usr-xyz987654321"};

        for (String userId : userIds) {
            String token = jwtService.generateToken("testuser", userId);
            String extractedUserId = jwtService.extractUserId(token);
            assertThat(extractedUserId).isEqualTo(userId);
        }
    }

    // --- isTokenValid Tests ---

    @Test
    @DisplayName("isTokenValid - Success (valid token)")
    void testIsTokenValid_Success() {
        // Given
        String username = "testuser";
        String userId = "usr-abcdefghijkl";
        String token = jwtService.generateToken(username, userId);

        // When
        boolean isValid = jwtService.isTokenValid(token, testUserDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid - Invalid (wrong username)")
    void testIsTokenValid_InvalidWrongUsername() {
        // Given
        String username = "testuser";
        String userId = "usr-abcdefghijkl";
        String token = jwtService.generateToken(username, userId);

        UserDetails differentUser = User.builder()
                .username("differentuser")
                .password("password")
                .build();

        // When
        boolean isValid = jwtService.isTokenValid(token, differentUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("isTokenValid - Invalid (expired token)")
    void testIsTokenValid_InvalidExpiredToken() {
        // Given - Create a token with very short expiration
        ReflectionTestUtils.setField(jwtService, "expiration", 1L); // 1 millisecond

        String username = "testuser";
        String userId = "usr-abcdefghijkl";
        String token = jwtService.generateToken(username, userId);

        // Wait for token to expire
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Reset expiration for other tests
        ReflectionTestUtils.setField(jwtService, "expiration", testExpiration);

        // When & Then - isTokenValid throws exception when token is expired (during extractUsername)
        // The JWT library throws ExpiredJwtException when parsing an expired token
        assertThatThrownBy(() -> jwtService.isTokenValid(token, testUserDetails))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("isTokenValid - Valid token with matching username")
    void testIsTokenValid_ValidTokenMatchingUsername() {
        // Given
        String username = "testuser";
        String userId = "usr-abcdefghijkl";
        String token = jwtService.generateToken(username, userId);

        // When
        boolean isValid = jwtService.isTokenValid(token, testUserDetails);

        // Then
        assertThat(isValid).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo(testUserDetails.getUsername());
    }

    // --- Token Structure Tests ---

    @Test
    @DisplayName("generateToken - Verify token structure (three parts)")
    void testGenerateToken_VerifyTokenStructure() {
        // Given
        String username = "testuser";
        String userId = "usr-abcdefghijkl";

        // When
        String token = jwtService.generateToken(username, userId);

        // Then - JWT tokens have three parts separated by dots
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3); // Header, Payload, Signature
    }

    @Test
    @DisplayName("generateToken - Verify token can be parsed")
    void testGenerateToken_VerifyTokenCanBeParsed() {
        // Given
        String username = "testuser";
        String userId = "usr-abcdefghijkl";
        String token = jwtService.generateToken(username, userId);

        // When - Extract claims directly using JWT library
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(testSecretKey));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Then
        assertThat(claims.getSubject()).isEqualTo(username);
        assertThat(claims.get("userId", String.class)).isEqualTo(userId);
        assertThat(claims.getExpiration()).isAfter(new Date());
    }
}
