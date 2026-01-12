package com.eaglebank.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_SECRET_KEY_LENGTH_BYTES = 32;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    @PostConstruct
    public void validateSecretKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            if (keyBytes.length < MIN_SECRET_KEY_LENGTH_BYTES) {
                throw new IllegalStateException(
                    String.format(
                        "JWT secret must be at least 256 bits (32 bytes). Current length: %d bits",
                        keyBytes.length * 8
                    )
                );
            }
            getSigningKey();
            log.info("JWT secret key validated successfully");
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT secret is not valid Base64 encoded string", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize JWT signing key", e);
        }
    }

    /**
     * Generates a JWT token for a user.
     *
     * @param username The username (used as subject for Spring Security compatibility)
     * @param userId The user ID (added as a custom claim)
     * @return The generated JWT token
     */
    public String generateToken(String username, String userId) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the username from a JWT token.
     *
     * @param token The JWT token
     * @return The username (subject claim)
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the user ID from a JWT token.
     *
     * @param token The JWT token
     * @return The user ID (userId claim)
     */
    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", String.class);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // New in JJWT 0.12+
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
