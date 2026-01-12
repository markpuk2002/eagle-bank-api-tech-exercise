package com.eaglebank.constants;

public final class ApiConstants {
    
    /**
     * Password pattern regex that requires:
     * - At least one lowercase letter (a-z)
     * - At least one uppercase letter (A-Z)
     * - At least one digit (0-9)
     * - At least one special character from: @#$%^&+=!\-_.,;:()[]{}|`~
     * - Minimum 8 characters total
     */
    public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!\\-_.,;:()\\[\\]{}|`~]).{8,}$";
    
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    
    public static final String USER_ID_PREFIX = "usr-";
    public static final String ACCOUNT_NUMBER_PREFIX = "01";
    public static final String TRANSACTION_ID_PREFIX = "tan-";
    
    public static final String DEFAULT_SORT_CODE = "10-10-10";
    
    public static final int MAX_ACCOUNT_GENERATION_ATTEMPTS = 100;
    public static final int ACCOUNT_NUMBER_RANDOM_DIGITS_MIN = 100000;
    public static final int ACCOUNT_NUMBER_RANDOM_DIGITS_MAX = 999999;
}
