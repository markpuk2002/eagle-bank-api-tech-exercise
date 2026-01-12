package com.eaglebank.mapper;

import com.eaglebank.dto.Address;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.request.CreateBankAccountRequest;
import com.eaglebank.dto.request.CreateTransactionRequest;
import com.eaglebank.dto.response.TransactionResponse;
import com.eaglebank.dto.response.UserResponse;
import com.eaglebank.entity.AddressEntity;
import com.eaglebank.entity.BankAccount;
import com.eaglebank.entity.Transaction;
import com.eaglebank.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper utility for converting between DTOs (Data Transfer Objects) and JPA entities.
 * Handles all entity-to-DTO and DTO-to-entity conversions with proper null handling.
 */
@Component
public class DtoMapper {

    /**
     * Converts an Address DTO to an AddressEntity for persistence.
     *
     * @param address The address DTO to convert
     * @return The AddressEntity, or null if the input address is null
     */
    public AddressEntity toAddressEntity(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressEntity(
            address.getLine1(),
            address.getLine2(),
            address.getLine3(),
            address.getTown(),
            address.getCounty(),
            address.getPostcode()
        );
    }

    /**
     * Converts an AddressEntity to an Address DTO for API responses.
     *
     * @param entity The AddressEntity to convert
     * @return The Address DTO, or null if the input entity is null
     */
    public Address toAddressDto(AddressEntity entity) {
        if (entity == null) {
            return null;
        }
        Address address = new Address();
        address.setLine1(entity.getLine1());
        address.setLine2(entity.getLine2());
        address.setLine3(entity.getLine3());
        address.setTown(entity.getTown());
        address.setCounty(entity.getCounty());
        address.setPostcode(entity.getPostcode());
        return address;
    }

    /**
     * Converts a User entity to a UserResponse DTO for API responses.
     * Note: Password is intentionally excluded for security reasons.
     *
     * @param user The User entity to convert
     * @return The UserResponse DTO, or null if the input user is null
     */
    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setAddress(toAddressDto(user.getAddress()));
        response.setPhoneNumber(user.getPhoneNumber());
        response.setEmail(user.getEmail());
        response.setCreatedTimestamp(user.getCreatedTimestamp());
        response.setUpdatedTimestamp(user.getUpdatedTimestamp());
        response.setUsername(user.getUsername());
        // Password intentionally excluded - never expose passwords in API responses
        return response;
    }

    /**
     * Converts a list of User entities to a list of UserResponse DTOs.
     *
     * @param users The list of User entities to convert
     * @return A list of UserResponse DTOs
     */
    public List<UserResponse> toUserResponseList(List<User> users) {
        return users.stream()
            .map(this::toUserResponse)
            .collect(Collectors.toList());
    }

    /**
     * Converts a BankAccount entity to a BankAccountResponse DTO for API responses.
     * Handles BigDecimal to Double conversion for balance field.
     *
     * @param account The BankAccount entity to convert
     * @return The BankAccountResponse DTO, or null if the input account is null
     */
    public BankAccountResponse toBankAccountResponse(BankAccount account) {
        if (account == null) {
            return null;
        }
        BankAccountResponse response = new BankAccountResponse();
        response.setAccountNumber(account.getAccountNumber());
        response.setSortCode(account.getSortCode());
        response.setName(account.getName());
        response.setAccountType(toAccountTypeDto(account.getAccountType()));
        response.setBalance(account.getBalance() != null ? account.getBalance().doubleValue() : 0.0);
        response.setCurrency(toCurrencyDto(account.getCurrency()));
        response.setCreatedTimestamp(account.getCreatedTimestamp());
        response.setUpdatedTimestamp(account.getUpdatedTimestamp());
        return response;
    }

    /**
     * Converts a list of BankAccount entities to a list of BankAccountResponse DTOs.
     *
     * @param accounts The list of BankAccount entities to convert
     * @return A list of BankAccountResponse DTOs
     */
    public List<BankAccountResponse> toBankAccountResponseList(List<BankAccount> accounts) {
        return accounts.stream()
            .map(this::toBankAccountResponse)
            .collect(Collectors.toList());
    }

    /**
     * Converts a BankAccount.AccountType enum to a CreateBankAccountRequest.AccountType enum.
     *
     * @param type The BankAccount.AccountType enum value
     * @return The corresponding CreateBankAccountRequest.AccountType enum value, or null if input is null
     */
    private CreateBankAccountRequest.AccountType toAccountTypeDto(BankAccount.AccountType type) {
        if (type == null) {
            return null;
        }
        return CreateBankAccountRequest.AccountType.valueOf(type.name());
    }

    /**
     * Converts a BankAccount.Currency enum to a BankAccountResponse.Currency enum.
     *
     * @param currency The BankAccount.Currency enum value
     * @return The corresponding BankAccountResponse.Currency enum value, or null if input is null
     */
    private BankAccountResponse.Currency toCurrencyDto(BankAccount.Currency currency) {
        if (currency == null) {
            return null;
        }
        return BankAccountResponse.Currency.valueOf(currency.name());
    }

    /**
     * Converts a Transaction entity to a TransactionResponse DTO for API responses.
     * Handles BigDecimal to Double conversion for amount field.
     *
     * @param transaction The Transaction entity to convert
     * @return The TransactionResponse DTO, or null if the input transaction is null
     */
    public TransactionResponse toTransactionResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setAmount(transaction.getAmount() != null ? transaction.getAmount().doubleValue() : 0.0);
        response.setCurrency(toCurrencyDto(transaction.getCurrency()));
        response.setType(toTransactionTypeDto(transaction.getType()));
        response.setReference(transaction.getReference());
        response.setUserId(transaction.getUser() != null ? transaction.getUser().getId() : null);
        response.setCreatedTimestamp(transaction.getCreatedTimestamp());
        return response;
    }

    /**
     * Converts a list of Transaction entities to a list of TransactionResponse DTOs.
     *
     * @param transactions The list of Transaction entities to convert
     * @return A list of TransactionResponse DTOs
     */
    public List<TransactionResponse> toTransactionResponseList(List<Transaction> transactions) {
        return transactions.stream()
            .map(this::toTransactionResponse)
            .collect(Collectors.toList());
    }

    /**
     * Converts a Transaction.TransactionType enum to a CreateTransactionRequest.TransactionType enum.
     *
     * @param type The Transaction.TransactionType enum value
     * @return The corresponding CreateTransactionRequest.TransactionType enum value, or null if input is null
     */
    private CreateTransactionRequest.TransactionType toTransactionTypeDto(Transaction.TransactionType type) {
        if (type == null) {
            return null;
        }
        return CreateTransactionRequest.TransactionType.valueOf(type.name());
    }
}