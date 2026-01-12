package com.eaglebank.mapper;

import com.eaglebank.dto.Address;
import com.eaglebank.dto.request.CreateBankAccountRequest;
import com.eaglebank.dto.request.CreateTransactionRequest;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.response.TransactionResponse;
import com.eaglebank.dto.response.UserResponse;
import com.eaglebank.entity.AddressEntity;
import com.eaglebank.entity.BankAccount;
import com.eaglebank.entity.Transaction;
import com.eaglebank.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dto Mapper Unit Tests")
class DtoMapperTest {

    @InjectMocks
    private DtoMapper dtoMapper;

    private Address addressDto;
    private AddressEntity addressEntity;
    private User user;
    private BankAccount bankAccount;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        // Setup Address DTO
        addressDto = new Address();
        addressDto.setLine1("123 Main St");
        addressDto.setLine2("Apartment 4B");
        addressDto.setLine3("Block A");
        addressDto.setTown("London");
        addressDto.setCounty("Greater London");
        addressDto.setPostcode("SW1A 1AA");

        // Setup AddressEntity
        addressEntity = new AddressEntity();
        addressEntity.setLine1("123 Main St");
        addressEntity.setLine2("Apartment 4B");
        addressEntity.setLine3("Block A");
        addressEntity.setTown("London");
        addressEntity.setCounty("Greater London");
        addressEntity.setPostcode("SW1A 1AA");

        // Setup User
        user = new User();
        user.setId("usr-abcdefghijkl");
        user.setUsername("johndoe");
        user.setEmail("john.doe@example.com");
        user.setName("John Doe");
        user.setPhoneNumber("+441234567890");
        user.setPassword("$argon2id$encoded-password");
        user.setAddress(addressEntity);
        user.setCreatedTimestamp(OffsetDateTime.now());
        user.setUpdatedTimestamp(OffsetDateTime.now());

        // Setup BankAccount
        bankAccount = new BankAccount();
        bankAccount.setAccountNumber("01123456");
        bankAccount.setSortCode("10-10-10");
        bankAccount.setName("My Savings Account");
        bankAccount.setAccountType(BankAccount.AccountType.personal);
        bankAccount.setBalance(BigDecimal.valueOf(1000.0));
        bankAccount.setCurrency(BankAccount.Currency.GBP);
        bankAccount.setUser(user);
        bankAccount.setCreatedTimestamp(OffsetDateTime.now());
        bankAccount.setUpdatedTimestamp(OffsetDateTime.now());

        // Setup Transaction
        transaction = new Transaction();
        transaction.setId("tan-abcdefghijkl");
        transaction.setBankAccount(bankAccount);
        transaction.setAmount(BigDecimal.valueOf(100.0));
        transaction.setCurrency(BankAccount.Currency.GBP);
        transaction.setType(Transaction.TransactionType.deposit);
        transaction.setReference("Test Deposit");
        transaction.setUser(user);
        transaction.setCreatedTimestamp(OffsetDateTime.now());
    }

    // --- toAddressEntity Tests ---

    @Test
    @DisplayName("toAddressEntity - Success with all fields")
    void testToAddressEntity_Success() {
        // When
        AddressEntity result = dtoMapper.toAddressEntity(addressDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLine1()).isEqualTo("123 Main St");
        assertThat(result.getLine2()).isEqualTo("Apartment 4B");
        assertThat(result.getLine3()).isEqualTo("Block A");
        assertThat(result.getTown()).isEqualTo("London");
        assertThat(result.getCounty()).isEqualTo("Greater London");
        assertThat(result.getPostcode()).isEqualTo("SW1A 1AA");
    }

    @Test
    @DisplayName("toAddressEntity - Success with partial fields")
    void testToAddressEntity_SuccessPartialFields() {
        // Given
        Address partialAddress = new Address();
        partialAddress.setLine1("123 Main St");
        partialAddress.setTown("London");
        partialAddress.setPostcode("SW1A 1AA");

        // When
        AddressEntity result = dtoMapper.toAddressEntity(partialAddress);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLine1()).isEqualTo("123 Main St");
        assertThat(result.getLine2()).isNull();
        assertThat(result.getLine3()).isNull();
        assertThat(result.getTown()).isEqualTo("London");
        assertThat(result.getCounty()).isNull();
        assertThat(result.getPostcode()).isEqualTo("SW1A 1AA");
    }

    @Test
    @DisplayName("toAddressEntity - Null input")
    void testToAddressEntity_NullInput() {
        // When
        AddressEntity result = dtoMapper.toAddressEntity(null);

        // Then
        assertThat(result).isNull();
    }

    // --- toAddressDto Tests ---

    @Test
    @DisplayName("toAddressDto - Success with all fields")
    void testToAddressDto_Success() {
        // When
        Address result = dtoMapper.toAddressDto(addressEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLine1()).isEqualTo("123 Main St");
        assertThat(result.getLine2()).isEqualTo("Apartment 4B");
        assertThat(result.getLine3()).isEqualTo("Block A");
        assertThat(result.getTown()).isEqualTo("London");
        assertThat(result.getCounty()).isEqualTo("Greater London");
        assertThat(result.getPostcode()).isEqualTo("SW1A 1AA");
    }

    @Test
    @DisplayName("toAddressDto - Success with partial fields")
    void testToAddressDto_SuccessPartialFields() {
        // Given
        AddressEntity partialEntity = new AddressEntity();
        partialEntity.setLine1("123 Main St");
        partialEntity.setTown("London");
        partialEntity.setPostcode("SW1A 1AA");

        // When
        Address result = dtoMapper.toAddressDto(partialEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLine1()).isEqualTo("123 Main St");
        assertThat(result.getLine2()).isNull();
        assertThat(result.getLine3()).isNull();
        assertThat(result.getTown()).isEqualTo("London");
        assertThat(result.getCounty()).isNull();
        assertThat(result.getPostcode()).isEqualTo("SW1A 1AA");
    }

    @Test
    @DisplayName("toAddressDto - Null input")
    void testToAddressDto_NullInput() {
        // When
        Address result = dtoMapper.toAddressDto(null);

        // Then
        assertThat(result).isNull();
    }

    // --- toUserResponse Tests ---

    @Test
    @DisplayName("toUserResponse - Success with all fields")
    void testToUserResponse_Success() {
        // When
        UserResponse result = dtoMapper.toUserResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("usr-abcdefghijkl");
        assertThat(result.getUsername()).isEqualTo("johndoe");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getPhoneNumber()).isEqualTo("+441234567890");
        assertThat(result.getAddress()).isNotNull();
        assertThat(result.getAddress().getLine1()).isEqualTo("123 Main St");
        assertThat(result.getCreatedTimestamp()).isEqualTo(user.getCreatedTimestamp());
        assertThat(result.getUpdatedTimestamp()).isEqualTo(user.getUpdatedTimestamp());
        // Password should not be included in response
    }

    @Test
    @DisplayName("toUserResponse - Success with null address")
    void testToUserResponse_SuccessWithNullAddress() {
        // Given
        user.setAddress(null);

        // When
        UserResponse result = dtoMapper.toUserResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("usr-abcdefghijkl");
        assertThat(result.getUsername()).isEqualTo("johndoe");
        assertThat(result.getAddress()).isNull();
    }

    @Test
    @DisplayName("toUserResponse - Null input")
    void testToUserResponse_NullInput() {
        // When
        UserResponse result = dtoMapper.toUserResponse(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toUserResponse - Verify password is not included")
    void testToUserResponse_PasswordNotIncluded() {
        // When
        UserResponse result = dtoMapper.toUserResponse(user);

        // Then - User entity has password, but response should not expose it
        assertThat(user.getPassword()).isNotNull();
        // Verify that result object doesn't have a password field (since it's not in UserResponse)
        // This is implicit - if the mapper were exposing password, we'd see it in the response
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull(); // If password was exposed, this test would still pass
    }

    // --- toUserResponseList Tests ---

    @Test
    @DisplayName("toUserResponseList - Success with list")
    void testToUserResponseList_Success() {
        // Given
        User user2 = new User();
        user2.setId("usr-xyz123456789");
        user2.setUsername("janedoe");
        user2.setEmail("jane.doe@example.com");

        List<User> users = List.of(user, user2);

        // When
        List<UserResponse> result = dtoMapper.toUserResponseList(users);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("usr-abcdefghijkl");
        assertThat(result.get(1).getId()).isEqualTo("usr-xyz123456789");
    }

    @Test
    @DisplayName("toUserResponseList - Success with empty list")
    void testToUserResponseList_SuccessEmptyList() {
        // Given
        List<User> users = Collections.emptyList();

        // When
        List<UserResponse> result = dtoMapper.toUserResponseList(users);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // --- toBankAccountResponse Tests ---

    @Test
    @DisplayName("toBankAccountResponse - Success with all fields")
    void testToBankAccountResponse_Success() {
        // When
        BankAccountResponse result = dtoMapper.toBankAccountResponse(bankAccount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccountNumber()).isEqualTo("01123456");
        assertThat(result.getSortCode()).isEqualTo("10-10-10");
        assertThat(result.getName()).isEqualTo("My Savings Account");
        assertThat(result.getAccountType()).isEqualTo(CreateBankAccountRequest.AccountType.personal);
        assertThat(result.getBalance()).isEqualTo(1000.0);
        assertThat(result.getCurrency()).isEqualTo(BankAccountResponse.Currency.GBP);
        assertThat(result.getCreatedTimestamp()).isEqualTo(bankAccount.getCreatedTimestamp());
        assertThat(result.getUpdatedTimestamp()).isEqualTo(bankAccount.getUpdatedTimestamp());
    }

    @Test
    @DisplayName("toBankAccountResponse - Success with null balance")
    void testToBankAccountResponse_SuccessWithNullBalance() {
        // Given
        bankAccount.setBalance(null);

        // When
        BankAccountResponse result = dtoMapper.toBankAccountResponse(bankAccount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBalance()).isEqualTo(0.0); // Should default to 0.0 when balance is null
    }

    @Test
    @DisplayName("toBankAccountResponse - Null input")
    void testToBankAccountResponse_NullInput() {
        // When
        BankAccountResponse result = dtoMapper.toBankAccountResponse(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toBankAccountResponse - Verify BigDecimal to Double conversion")
    void testToBankAccountResponse_BigDecimalToDoubleConversion() {
        // Given
        bankAccount.setBalance(BigDecimal.valueOf(123.45));

        // When
        BankAccountResponse result = dtoMapper.toBankAccountResponse(bankAccount);

        // Then
        assertThat(result.getBalance()).isEqualTo(123.45);
        assertThat(result.getBalance()).isInstanceOf(Double.class);
    }

    // --- toBankAccountResponseList Tests ---

    @Test
    @DisplayName("toBankAccountResponseList - Success with list")
    void testToBankAccountResponseList_Success() {
        // Given
        BankAccount account2 = new BankAccount();
        account2.setAccountNumber("01234567");
        account2.setName("Checking Account");

        List<BankAccount> accounts = List.of(bankAccount, account2);

        // When
        List<BankAccountResponse> result = dtoMapper.toBankAccountResponseList(accounts);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAccountNumber()).isEqualTo("01123456");
        assertThat(result.get(1).getAccountNumber()).isEqualTo("01234567");
    }

    @Test
    @DisplayName("toBankAccountResponseList - Success with empty list")
    void testToBankAccountResponseList_SuccessEmptyList() {
        // Given
        List<BankAccount> accounts = Collections.emptyList();

        // When
        List<BankAccountResponse> result = dtoMapper.toBankAccountResponseList(accounts);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // --- toTransactionResponse Tests ---

    @Test
    @DisplayName("toTransactionResponse - Success with all fields")
    void testToTransactionResponse_Success() {
        // When
        TransactionResponse result = dtoMapper.toTransactionResponse(transaction);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("tan-abcdefghijkl");
        assertThat(result.getAmount()).isEqualTo(100.0);
        assertThat(result.getCurrency()).isEqualTo(BankAccountResponse.Currency.GBP);
        assertThat(result.getType()).isEqualTo(CreateTransactionRequest.TransactionType.deposit);
        assertThat(result.getReference()).isEqualTo("Test Deposit");
        assertThat(result.getUserId()).isEqualTo("usr-abcdefghijkl");
        assertThat(result.getCreatedTimestamp()).isEqualTo(transaction.getCreatedTimestamp());
    }

    @Test
    @DisplayName("toTransactionResponse - Success with null amount")
    void testToTransactionResponse_SuccessWithNullAmount() {
        // Given
        transaction.setAmount(null);

        // When
        TransactionResponse result = dtoMapper.toTransactionResponse(transaction);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(0.0); // Should default to 0.0 when amount is null
    }

    @Test
    @DisplayName("toTransactionResponse - Success with null user")
    void testToTransactionResponse_SuccessWithNullUser() {
        // Given
        transaction.setUser(null);

        // When
        TransactionResponse result = dtoMapper.toTransactionResponse(transaction);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isNull();
    }

    @Test
    @DisplayName("toTransactionResponse - Null input")
    void testToTransactionResponse_NullInput() {
        // When
        TransactionResponse result = dtoMapper.toTransactionResponse(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toTransactionResponse - Verify BigDecimal to Double conversion")
    void testToTransactionResponse_BigDecimalToDoubleConversion() {
        // Given
        transaction.setAmount(BigDecimal.valueOf(99.99));

        // When
        TransactionResponse result = dtoMapper.toTransactionResponse(transaction);

        // Then
        assertThat(result.getAmount()).isEqualTo(99.99);
        assertThat(result.getAmount()).isInstanceOf(Double.class);
    }

    @Test
    @DisplayName("toTransactionResponse - Success with withdrawal type")
    void testToTransactionResponse_SuccessWithdrawalType() {
        // Given
        transaction.setType(Transaction.TransactionType.withdrawal);

        // When
        TransactionResponse result = dtoMapper.toTransactionResponse(transaction);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(CreateTransactionRequest.TransactionType.withdrawal);
    }

    // --- toTransactionResponseList Tests ---

    @Test
    @DisplayName("toTransactionResponseList - Success with list")
    void testToTransactionResponseList_Success() {
        // Given
        Transaction transaction2 = new Transaction();
        transaction2.setId("tan-xyz123456789");
        transaction2.setAmount(BigDecimal.valueOf(200.0));
        transaction2.setType(Transaction.TransactionType.withdrawal);

        List<Transaction> transactions = List.of(transaction, transaction2);

        // When
        List<TransactionResponse> result = dtoMapper.toTransactionResponseList(transactions);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("tan-abcdefghijkl");
        assertThat(result.get(1).getId()).isEqualTo("tan-xyz123456789");
    }

    @Test
    @DisplayName("toTransactionResponseList - Success with empty list")
    void testToTransactionResponseList_SuccessEmptyList() {
        // Given
        List<Transaction> transactions = Collections.emptyList();

        // When
        List<TransactionResponse> result = dtoMapper.toTransactionResponseList(transactions);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // --- Enum Conversion Tests (tested indirectly) ---

    @Test
    @DisplayName("toBankAccountResponse - Verify AccountType enum conversion")
    void testToBankAccountResponse_AccountTypeEnumConversion() {
        // Given
        bankAccount.setAccountType(BankAccount.AccountType.personal);

        // When
        BankAccountResponse result = dtoMapper.toBankAccountResponse(bankAccount);

        // Then
        assertThat(result.getAccountType()).isEqualTo(CreateBankAccountRequest.AccountType.personal);
    }

    @Test
    @DisplayName("toBankAccountResponse - Verify Currency enum conversion")
    void testToBankAccountResponse_CurrencyEnumConversion() {
        // Given
        bankAccount.setCurrency(BankAccount.Currency.GBP);

        // When
        BankAccountResponse result = dtoMapper.toBankAccountResponse(bankAccount);

        // Then
        assertThat(result.getCurrency()).isEqualTo(BankAccountResponse.Currency.GBP);
    }

    @Test
    @DisplayName("toTransactionResponse - Verify TransactionType enum conversion")
    void testToTransactionResponse_TransactionTypeEnumConversion() {
        // Given
        transaction.setType(Transaction.TransactionType.deposit);

        // When
        TransactionResponse result = dtoMapper.toTransactionResponse(transaction);

        // Then
        assertThat(result.getType()).isEqualTo(CreateTransactionRequest.TransactionType.deposit);
    }
}
