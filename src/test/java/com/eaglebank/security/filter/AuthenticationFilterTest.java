package com.eaglebank.security.filter;

import com.eaglebank.constants.ApiConstants;
import com.eaglebank.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication Filter Unit Tests")
class AuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private AuthenticationFilter authenticationFilter;

    private UserDetails testUserDetails;
    private String testToken;
    private String testUsername;

    @BeforeEach
    void setUp() {
        // Clear SecurityContext before each test
        SecurityContextHolder.clearContext();

        testUsername = "testuser";
        testToken = "test-jwt-token";
        testUserDetails = User.builder()
                .username(testUsername)
                .password("password")
                .build();
    }

    // --- doFilterInternal Tests ---

    @Test
    @DisplayName("doFilterInternal - Success with valid token")
    void testDoFilterInternal_SuccessWithValidToken() throws ServletException, IOException {
        // Given
        String authHeader = ApiConstants.BEARER_PREFIX + testToken;
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authHeader);
        when(jwtService.extractUsername(testToken)).thenReturn(testUsername);
        when(userDetailsService.loadUserByUsername(testUsername)).thenReturn(testUserDetails);
        when(jwtService.isTokenValid(testToken, testUserDetails)).thenReturn(true);

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService, times(1)).extractUsername(testToken);
        verify(userDetailsService, times(1)).loadUserByUsername(testUsername);
        verify(jwtService, times(1)).isTokenValid(testToken, testUserDetails);
        verify(filterChain, times(1)).doFilter(request, response);

        // Verify authentication was set in SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(testUserDetails);
    }

    @Test
    @DisplayName("doFilterInternal - No Authorization header")
    void testDoFilterInternal_NoAuthorizationHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(null);

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService, never()).extractUsername(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain, times(1)).doFilter(request, response);

        // Verify no authentication was set
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("doFilterInternal - Authorization header without Bearer prefix")
    void testDoFilterInternal_NoBearerPrefix() throws ServletException, IOException {
        // Given
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn("InvalidToken");

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService, never()).extractUsername(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Empty Bearer token")
    void testDoFilterInternal_EmptyBearerToken() throws ServletException, IOException {
        // Given
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(ApiConstants.BEARER_PREFIX);
        when(jwtService.extractUsername("")).thenReturn(null);

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService, times(1)).extractUsername("");
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Null username extracted from token")
    void testDoFilterInternal_NullUsername() throws ServletException, IOException {
        // Given
        String authHeader = ApiConstants.BEARER_PREFIX + testToken;
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authHeader);
        when(jwtService.extractUsername(testToken)).thenReturn(null);

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService, times(1)).extractUsername(testToken);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Invalid token")
    void testDoFilterInternal_InvalidToken() throws ServletException, IOException {
        // Given
        String authHeader = ApiConstants.BEARER_PREFIX + testToken;
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authHeader);
        when(jwtService.extractUsername(testToken)).thenReturn(testUsername);
        when(userDetailsService.loadUserByUsername(testUsername)).thenReturn(testUserDetails);
        when(jwtService.isTokenValid(testToken, testUserDetails)).thenReturn(false);

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService, times(1)).extractUsername(testToken);
        verify(userDetailsService, times(1)).loadUserByUsername(testUsername);
        verify(jwtService, times(1)).isTokenValid(testToken, testUserDetails);
        verify(filterChain, times(1)).doFilter(request, response);

        // Verify no authentication was set (token is invalid)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("doFilterInternal - Authentication already set in context")
    void testDoFilterInternal_AuthenticationAlreadySet() throws ServletException, IOException {
        // Given - Set authentication in context first
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication existingAuth = mock(Authentication.class);
        context.setAuthentication(existingAuth);
        SecurityContextHolder.setContext(context);

        String authHeader = ApiConstants.BEARER_PREFIX + testToken;
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authHeader);
        when(jwtService.extractUsername(testToken)).thenReturn(testUsername);

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService, times(1)).extractUsername(testToken);
        verify(userDetailsService, never()).loadUserByUsername(anyString()); // Should not load user if auth already exists
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Exception during token validation")
    void testDoFilterInternal_ExceptionDuringValidation() throws ServletException, IOException {
        // Given
        String authHeader = ApiConstants.BEARER_PREFIX + testToken;
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authHeader);
        when(jwtService.extractUsername(testToken)).thenThrow(new RuntimeException("Token parsing error"));

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then - Should continue filter chain even if exception occurs
        verify(jwtService, times(1)).extractUsername(testToken);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Verify token extraction removes Bearer prefix")
    void testDoFilterInternal_VerifyTokenExtraction() throws ServletException, IOException {
        // Given
        String authHeader = ApiConstants.BEARER_PREFIX + testToken;
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authHeader);
        when(jwtService.extractUsername(testToken)).thenReturn(testUsername);
        when(userDetailsService.loadUserByUsername(testUsername)).thenReturn(testUserDetails);
        when(jwtService.isTokenValid(testToken, testUserDetails)).thenReturn(true);

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then - Verify JWT service is called with token (without Bearer prefix)
        verify(jwtService, times(1)).extractUsername(eq(testToken));
        verify(jwtService, never()).extractUsername(eq(authHeader)); // Should not pass full header
    }
}
