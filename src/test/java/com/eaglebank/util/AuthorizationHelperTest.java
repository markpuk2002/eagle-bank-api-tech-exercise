package com.eaglebank.util;

import com.eaglebank.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Authorization Helper Unit Tests")
class AuthorizationHelperTest {

    private static final String TEST_USER_ID = "usr-abcdefghijkl";
    private static final String OTHER_USER_ID = "usr-xyz123456789";
    private static final String RESOURCE_TYPE = "bank account";

    // --- verifyUserOwnership Tests ---

    @Test
    @DisplayName("verifyUserOwnership - Success (same user)")
    void testVerifyUserOwnership_Success() {
        // When & Then - Should not throw exception
        AuthorizationHelper.verifyUserOwnership(TEST_USER_ID, TEST_USER_ID, RESOURCE_TYPE);
    }

    @Test
    @DisplayName("verifyUserOwnership - Throws UnauthorizedException (different user)")
    void testVerifyUserOwnership_DifferentUser() {
        // When & Then
        assertThatThrownBy(() -> AuthorizationHelper.verifyUserOwnership(TEST_USER_ID, OTHER_USER_ID, RESOURCE_TYPE))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User " + OTHER_USER_ID + " does not have access to this " + RESOURCE_TYPE);
    }

    @Test
    @DisplayName("verifyUserOwnership - Throws IllegalArgumentException (null resource owner ID)")
    void testVerifyUserOwnership_NullResourceOwnerId() {
        // When & Then
        assertThatThrownBy(() -> AuthorizationHelper.verifyUserOwnership(null, TEST_USER_ID, RESOURCE_TYPE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resource owner ID cannot be null");
    }

    @Test
    @DisplayName("verifyUserOwnership - Throws IllegalArgumentException (null user ID)")
    void testVerifyUserOwnership_NullUserId() {
        // When & Then
        assertThatThrownBy(() -> AuthorizationHelper.verifyUserOwnership(TEST_USER_ID, null, RESOURCE_TYPE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null");
    }

    @Test
    @DisplayName("verifyUserOwnership - Verify message format for different resource types")
    void testVerifyUserOwnership_VerifyMessageFormat() {
        // Given
        String[] resourceTypes = {"bank account", "transaction", "user profile"};

        for (String resourceType : resourceTypes) {
            // When & Then
            assertThatThrownBy(() -> AuthorizationHelper.verifyUserOwnership(TEST_USER_ID, OTHER_USER_ID, resourceType))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("User " + OTHER_USER_ID + " does not have access to this " + resourceType);
        }
    }

    @Test
    @DisplayName("verifyUserOwnership - Verify case sensitivity")
    void testVerifyUserOwnership_CaseSensitive() {
        // Given - Different case user IDs
        String userId1 = "usr-ABCDEFGHIJKL";
        String userId2 = "usr-abcdefghijkl";

        // When & Then - Should throw exception as IDs are different (case-sensitive)
        assertThatThrownBy(() -> AuthorizationHelper.verifyUserOwnership(userId1, userId2, RESOURCE_TYPE))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("does not have access to this " + RESOURCE_TYPE);
    }

    @Test
    @DisplayName("verifyUserOwnership - Verify with empty strings")
    void testVerifyUserOwnership_EmptyStrings() {
        // Given
        String emptyUserId1 = "";
        String emptyUserId2 = "";

        // When & Then - Empty strings are equal, so should not throw
        AuthorizationHelper.verifyUserOwnership(emptyUserId1, emptyUserId2, RESOURCE_TYPE);
    }

    @Test
    @DisplayName("verifyUserOwnership - Verify with different empty strings")
    void testVerifyUserOwnership_DifferentEmptyStrings() {
        // Given
        String emptyUserId1 = "";
        String emptyUserId2 = " ";

        // When & Then - Different strings, should throw
        assertThatThrownBy(() -> AuthorizationHelper.verifyUserOwnership(emptyUserId1, emptyUserId2, RESOURCE_TYPE))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("does not have access to this " + RESOURCE_TYPE);
    }
}
