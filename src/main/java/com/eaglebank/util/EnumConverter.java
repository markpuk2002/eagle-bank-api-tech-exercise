package com.eaglebank.util;

import com.eaglebank.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for safe enum conversions with proper error handling.
 * Provides reusable methods to convert enum values with validation and exception handling.
 */
public final class EnumConverter {

    private static final Logger log = LoggerFactory.getLogger(EnumConverter.class);

    /**
     * Safely converts an enum value by name, throwing a ValidationException if the conversion fails.
     *
     * @param <T> The enum type
     * @param enumClass The enum class to convert to
     * @param value The enum value to convert (typically from a DTO)
     * @param valueName The name of the value for error messages (e.g., "transaction type", "currency")
     * @return The converted enum value
     * @throws ValidationException If the enum value is invalid
     */
    public static <T extends Enum<T>> T convertEnum(Class<T> enumClass, Enum<?> value, String valueName) {
        if (value == null) {
            throw new ValidationException(valueName + " cannot be null");
        }
        
        try {
            return Enum.valueOf(enumClass, value.name());
        } catch (IllegalArgumentException e) {
            log.error("Invalid {}: {}", valueName, value);
            throw new ValidationException(
                String.format("Invalid %s: %s", valueName, value),
                e
            );
        }
    }
}
