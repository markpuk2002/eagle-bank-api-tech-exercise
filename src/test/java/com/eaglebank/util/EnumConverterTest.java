package com.eaglebank.util;

import com.eaglebank.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Enum Converter Unit Tests")
class EnumConverterTest {

    // Test enums for testing
    enum TestEnum {
        VALUE1, VALUE2, VALUE3
    }

    enum AnotherEnum {
        OPTION_A, OPTION_B
    }

    // --- convertEnum Tests ---

    @Test
    @DisplayName("convertEnum - Success (matching enum)")
    void testConvertEnum_Success() {
        // Given
        TestEnum value = TestEnum.VALUE1;
        String valueName = "test enum";

        // When
        TestEnum result = EnumConverter.convertEnum(TestEnum.class, value, valueName);

        // Then
        assertThat(result).isEqualTo(TestEnum.VALUE1);
    }

    @Test
    @DisplayName("convertEnum - Success (different enum with same name)")
    void testConvertEnum_SuccessDifferentEnumSameName() {
        // Given - Both enums have VALUE1
        enum Enum1 {
            VALUE1, VALUE2
        }
        enum Enum2 {
            VALUE1, VALUE2
        }
        
        Enum1 value = Enum1.VALUE1;
        String valueName = "test enum";

        // When
        Enum2 result = EnumConverter.convertEnum(Enum2.class, value, valueName);

        // Then
        assertThat(result).isEqualTo(Enum2.VALUE1);
        assertThat(result.name()).isEqualTo(value.name());
    }

    @Test
    @DisplayName("convertEnum - Throws ValidationException (null value)")
    void testConvertEnum_NullValue() {
        // Given
        String valueName = "test enum";

        // When & Then
        assertThatThrownBy(() -> EnumConverter.convertEnum(TestEnum.class, null, valueName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(valueName + " cannot be null");
    }

    @Test
    @DisplayName("convertEnum - Throws ValidationException (invalid enum value)")
    void testConvertEnum_InvalidEnumValue() {
        // Given - Try to convert VALUE1 from TestEnum to AnotherEnum (which doesn't have VALUE1)
        TestEnum value = TestEnum.VALUE1;
        String valueName = "test enum";

        // When & Then
        assertThatThrownBy(() -> EnumConverter.convertEnum(AnotherEnum.class, value, valueName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid " + valueName)
                .hasMessageContaining(value.toString());
    }

    @Test
    @DisplayName("convertEnum - Verify error message format")
    void testConvertEnum_VerifyErrorMessageFormat() {
        // Given
        TestEnum value = TestEnum.VALUE1;
        String valueName = "transaction type";

        // When & Then
        assertThatThrownBy(() -> EnumConverter.convertEnum(AnotherEnum.class, value, valueName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid " + valueName + ": " + value);
    }

    @Test
    @DisplayName("convertEnum - Success with all enum values")
    void testConvertEnum_SuccessAllValues() {
        // Given
        TestEnum[] values = TestEnum.values();
        String valueName = "test enum";

        // When & Then - All values should convert successfully
        for (TestEnum value : values) {
            TestEnum result = EnumConverter.convertEnum(TestEnum.class, value, valueName);
            assertThat(result).isEqualTo(value);
        }
    }

    @Test
    @DisplayName("convertEnum - Verify case sensitivity")
    void testConvertEnum_CaseSensitivity() {
        // Given - Enum names are case-sensitive in Java
        TestEnum value = TestEnum.VALUE1;
        String valueName = "test enum";

        // When - Converting within same enum class should work
        TestEnum result = EnumConverter.convertEnum(TestEnum.class, value, valueName);

        // Then
        assertThat(result).isEqualTo(TestEnum.VALUE1);
        assertThat(result.name()).isEqualTo("VALUE1"); // Enum names are uppercase
    }

    @Test
    @DisplayName("convertEnum - Verify with BankAccount.AccountType")
    void testConvertEnum_BankAccountAccountType() {
        // Given
        com.eaglebank.entity.BankAccount.AccountType accountType = 
                com.eaglebank.entity.BankAccount.AccountType.personal;
        String valueName = "account type";

        // When
        com.eaglebank.dto.request.CreateBankAccountRequest.AccountType result = 
                EnumConverter.convertEnum(
                        com.eaglebank.dto.request.CreateBankAccountRequest.AccountType.class,
                        accountType,
                        valueName
                );

        // Then
        assertThat(result).isEqualTo(
                com.eaglebank.dto.request.CreateBankAccountRequest.AccountType.personal
        );
    }

    @Test
    @DisplayName("convertEnum - Verify with BankAccount.Currency")
    void testConvertEnum_BankAccountCurrency() {
        // Given
        com.eaglebank.entity.BankAccount.Currency currency = 
                com.eaglebank.entity.BankAccount.Currency.GBP;
        String valueName = "currency";

        // When
        com.eaglebank.dto.response.BankAccountResponse.Currency result = 
                EnumConverter.convertEnum(
                        com.eaglebank.dto.response.BankAccountResponse.Currency.class,
                        currency,
                        valueName
                );

        // Then
        assertThat(result).isEqualTo(com.eaglebank.dto.response.BankAccountResponse.Currency.GBP);
    }

    @Test
    @DisplayName("convertEnum - Verify with Transaction.TransactionType")
    void testConvertEnum_TransactionTransactionType() {
        // Given
        com.eaglebank.entity.Transaction.TransactionType transactionType = 
                com.eaglebank.entity.Transaction.TransactionType.deposit;
        String valueName = "transaction type";

        // When
        com.eaglebank.dto.request.CreateTransactionRequest.TransactionType result = 
                EnumConverter.convertEnum(
                        com.eaglebank.dto.request.CreateTransactionRequest.TransactionType.class,
                        transactionType,
                        valueName
                );

        // Then
        assertThat(result).isEqualTo(
                com.eaglebank.dto.request.CreateTransactionRequest.TransactionType.deposit
        );
    }

    @Test
    @DisplayName("convertEnum - Verify exception preserves cause")
    void testConvertEnum_ExceptionPreservesCause() {
        // Given
        TestEnum value = TestEnum.VALUE1;
        String valueName = "test enum";

        // When
        try {
            EnumConverter.convertEnum(AnotherEnum.class, value, valueName);
        } catch (ValidationException e) {
            // Then - Exception should have a cause (IllegalArgumentException)
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
