package com.eaglebank.util;

import com.eaglebank.exception.UnauthorizedException;

/**
 * Utility class for authorization checks and user ownership verification.
 * Provides reusable methods to ensure consistent authorization logic across the application.
 */
public final class AuthorizationHelper {

    /**
     * Verifies that the specified user owns or has access to a resource.
     *
     * @param resourceOwnerId The ID of the user who owns the resource
     * @param userId The ID of the user making the request (from authentication)
     * @param resourceType The type of resource being accessed (e.g., "bank account", "transaction")
     * @throws IllegalArgumentException If either user ID is null
     * @throws UnauthorizedException If the user does not own or have access to the resource
     */
    public static void verifyUserOwnership(String resourceOwnerId, String userId, String resourceType) {
        if (resourceOwnerId == null) {
            throw new IllegalArgumentException("Resource owner ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (!resourceOwnerId.equals(userId)) {
            throw new UnauthorizedException(
                String.format("User %s does not have access to this %s", userId, resourceType)
            );
        }
    }
}
