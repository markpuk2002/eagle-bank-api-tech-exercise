package com.eaglebank.service;

import com.eaglebank.constants.ApiConstants;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.request.CreateBankAccountRequest;
import com.eaglebank.dto.response.ListBankAccountsResponse;
import com.eaglebank.dto.request.UpdateBankAccountRequest;
import com.eaglebank.entity.BankAccount;
import com.eaglebank.entity.User;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.exception.UnauthorizedException;
import com.eaglebank.util.EnumConverter;
import com.eaglebank.mapper.DtoMapper;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.util.AuthorizationHelper;
import com.eaglebank.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for managing bank accounts.
 * Handles account creation, retrieval, update, deletion, and ownership verification.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final BankAccount.Currency DEFAULT_CURRENCY = BankAccount.Currency.GBP;

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final DtoMapper dtoMapper;
    private final IdGenerator idGenerator;

    /**
     * Creates a new bank account for the specified user.
     *
     * @param request The bank account creation request containing account details (name, account type)
     * @param userId The ID of the user who will own the account (must match authenticated user)
     * @return The created bank account response with generated account number and initial balance
     * @throws ResourceNotFoundException If the user with the specified ID does not exist
     * @throws com.eaglebank.exception.ValidationException If invalid account type is provided
     */
    public BankAccountResponse createAccount(CreateBankAccountRequest request, String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String accountNumber = idGenerator.generateAccountNumber();

        BankAccount account = new BankAccount();
        account.setAccountNumber(accountNumber);
        account.setSortCode(ApiConstants.DEFAULT_SORT_CODE);
        account.setName(request.getName());
        account.setAccountType(EnumConverter.convertEnum(
            BankAccount.AccountType.class,
            request.getAccountType(),
            "account type"
        ));
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency(DEFAULT_CURRENCY);
        account.setUser(user);

        BankAccount savedAccount = bankAccountRepository.save(account);
        log.info("Bank account created successfully: {} for user: {}", accountNumber, userId);
        return dtoMapper.toBankAccountResponse(savedAccount);
    }

    /**
     * Retrieves all bank accounts owned by the specified user.
     *
     * @param userId The ID of the user whose accounts to retrieve
     * @return A list of all bank accounts owned by the user
     */
    @Transactional(readOnly = true)
    public ListBankAccountsResponse listAccounts(String userId) {
        List<BankAccount> accounts = bankAccountRepository.findByUser_Id(userId);
        ListBankAccountsResponse response = new ListBankAccountsResponse();
        response.setAccounts(dtoMapper.toBankAccountResponseList(accounts));
        return response;
    }

    /**
     * Retrieves a bank account by account number after verifying user ownership.
     *
     * @param accountNumber The account number to retrieve (format: 01xxxxxx)
     * @param userId The ID of the user making the request (must own the account)
     * @return The bank account response
     * @throws ResourceNotFoundException If the bank account with the specified account number does not exist
     * @throws UnauthorizedException If the user does not own the bank account
     */
    @Transactional(readOnly = true)
    public BankAccountResponse getAccountByAccountNumber(String accountNumber, String userId) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Bank account", accountNumber));
        
        AuthorizationHelper.verifyUserOwnership(account.getUser().getId(), userId, "bank account");
        
        return dtoMapper.toBankAccountResponse(account);
    }

    /**
     * Updates an existing bank account (name and/or account type).
     *
     * @param accountNumber The account number to update (format: 01xxxxxx)
     * @param request The update request containing new values (only non-null fields will be updated)
     * @param userId The ID of the user making the request (must own the account)
     * @return The updated bank account response
     * @throws ResourceNotFoundException If the bank account with the specified account number does not exist
     * @throws UnauthorizedException If the user does not own the bank account
     * @throws com.eaglebank.exception.ValidationException If an invalid account type is provided
     */
    public BankAccountResponse updateAccount(String accountNumber, UpdateBankAccountRequest request, String userId) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Bank account", accountNumber));

        AuthorizationHelper.verifyUserOwnership(account.getUser().getId(), userId, "bank account");

        if (request.getName() != null) {
            account.setName(request.getName());
        }
        if (request.getAccountType() != null) {
            account.setAccountType(EnumConverter.convertEnum(
                BankAccount.AccountType.class,
                request.getAccountType(),
                "account type"
            ));
        }

        BankAccount updatedAccount = bankAccountRepository.save(account);
        return dtoMapper.toBankAccountResponse(updatedAccount);
    }

    /**
     * Deletes a bank account after verifying user ownership.
     *
     * @param accountNumber The account number to delete (format: 01xxxxxx)
     * @param userId The ID of the user making the request (must own the account)
     * @throws ResourceNotFoundException If the bank account with the specified account number does not exist
     * @throws UnauthorizedException If the user does not own the bank account
     */
    public void deleteAccount(String accountNumber, String userId) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Bank account", accountNumber));
        
        AuthorizationHelper.verifyUserOwnership(account.getUser().getId(), userId, "bank account");
        
        bankAccountRepository.deleteById(accountNumber);
        log.info("Bank account deleted successfully: {} by user: {}", accountNumber, userId);
    }
}