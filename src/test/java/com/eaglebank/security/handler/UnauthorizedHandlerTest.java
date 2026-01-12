package com.eaglebank.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unauthorized Handler Unit Tests")
class UnauthorizedHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException authException;

    @InjectMocks
    private UnauthorizedHandler unauthorizedHandler;

    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws IOException {
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);
    }

    // --- commence Tests ---

    @Test
    @DisplayName("commence - Success")
    void testCommence_Success() throws IOException {
        // Given
        when(authException.getMessage()).thenReturn("Access denied");

        // When
        unauthorizedHandler.commence(request, response, authException);

        // Then
        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response, times(1)).setContentType("application/json");
        verify(response, times(1)).getWriter();

        String responseBody = stringWriter.toString();
        assertThat(responseBody).contains("Unauthorized");
        assertThat(responseBody).contains("Access denied");
    }

    @Test
    @DisplayName("commence - Verify status code is 401")
    void testCommence_VerifyStatusCode() throws IOException {
        // Given
        when(authException.getMessage()).thenReturn("Invalid credentials");

        // When
        unauthorizedHandler.commence(request, response, authException);

        // Then
        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("commence - Verify content type is application/json")
    void testCommence_VerifyContentType() throws IOException {
        // Given
        when(authException.getMessage()).thenReturn("Invalid token");

        // When
        unauthorizedHandler.commence(request, response, authException);

        // Then
        verify(response, times(1)).setContentType("application/json");
    }

    @Test
    @DisplayName("commence - Verify response body format")
    void testCommence_VerifyResponseBodyFormat() throws IOException {
        // Given
        String errorMessage = "Authentication failed";
        when(authException.getMessage()).thenReturn(errorMessage);

        // When
        unauthorizedHandler.commence(request, response, authException);

        // Then
        String responseBody = stringWriter.toString();
        assertThat(responseBody).contains("\"error\":");
        assertThat(responseBody).contains("\"message\":");
        assertThat(responseBody).contains("Unauthorized");
        assertThat(responseBody).contains(errorMessage);
    }

    @Test
    @DisplayName("commence - Verify exception message is included")
    void testCommence_VerifyExceptionMessageIncluded() throws IOException {
        // Given
        String[] messages = {"Invalid token", "Access denied", "Authentication required"};

        for (String message : messages) {
            when(authException.getMessage()).thenReturn(message);
            stringWriter.getBuffer().setLength(0); // Clear buffer

            // When
            unauthorizedHandler.commence(request, response, authException);

            // Then
            String responseBody = stringWriter.toString();
            assertThat(responseBody).contains(message);
        }
    }

    @Test
    @DisplayName("commence - Verify JSON structure")
    void testCommence_VerifyJsonStructure() throws IOException {
        // Given
        when(authException.getMessage()).thenReturn("Test message");

        // When
        unauthorizedHandler.commence(request, response, authException);

        // Then
        String responseBody = stringWriter.toString();
        // Basic JSON structure check
        assertThat(responseBody).startsWith("{");
        assertThat(responseBody).endsWith("}");
        assertThat(responseBody).contains("\"error\"");
        assertThat(responseBody).contains("\"message\"");
    }
}
