package com.eaglebank.service;

import com.eaglebank.constants.ApiConstants;
import com.eaglebank.dto.request.CreateBankAccountRequest;
import com.eaglebank.dto.request.UpdateBankAccountRequest;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.response.ListBankAccountsResponse;
import com.eaglebank.entity.BankAccount;
import com.eaglebank.entity.User;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.exception.UnauthorizedException;
import com.eaglebank.mapper.DtoMapper;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Service Unit Tests")
class AccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private AccountService accountService;

    private CreateBankAccountRequest createBankAccountRequest;
    private UpdateBankAccountRequest updateBankAccountRequest;
    private BankAccountResponse bankAccountResponse;
    private BankAccount testBankAccount;
    private User testUser;
    private String testUserId;
    private String testAccountNumber;
    private String otherUserId;

    @BeforeEach
    void setUp() {
        testUserId = "usr-abcdefghijkl";
        otherUserId = "usr-xyz123456789";
        testAccountNumber = "01123456";

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testBankAccount = new BankAccount();
        testBankAccount.setAccountNumber(testAccountNumber);
        testBankAccount.setSortCode(ApiConstants.DEFAULT_SORT_CODE);
        testBankAccount.setName("My Savings Account");
        testBankAccount.setAccountType(BankAccount.AccountType.personal);
        testBankAccount.setBalance(BigDecimal.valueOf(1000.0));
        testBankAccount.setCurrency(BankAccount.Currency.GBP);
        testBankAccount.setUser(testUser);
        testBankAccount.setCreatedTimestamp(OffsetDateTime.now());
        testBankAccount.setUpdatedTimestamp(OffsetDateTime.now());

        createBankAccountRequest = new CreateBankAccountRequest();
        createBankAccountRequest.setName("My Savings Account");
        createBankAccountRequest.setAccountType(CreateBankAccountRequest.AccountType.personal);

        updateBankAccountRequest = new UpdateBankAccountRequest();
        updateBankAccountRequest.setName("My Updated Account");
        updateBankAccountRequest.setAccountType(CreateBankAccountRequest.AccountType.personal);

        bankAccountResponse = new BankAccountResponse();
        bankAccountResponse.setAccountNumber(testAccountNumber);
        bankAccountResponse.setSortCode("10-10-10");
        bankAccountResponse.setName("My Savings Account");
        bankAccountResponse.setAccountType(CreateBankAccountRequest.AccountType.personal);
        bankAccountResponse.setBalance(1000.0);
        bankAccountResponse.setCurrency(BankAccountResponse.Currency.GBP);
        bankAccountResponse.setCreatedTimestamp(OffsetDateTime.now());
        bankAccountResponse.setUpdatedTimestamp(OffsetDateTime.now());
    }

    // --- createAccount Tests ---

    @Test
    @DisplayName("createAccount - Success")
    void testCreateAccount_Success() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(idGenerator.generateAccountNumber()).thenReturn(testAccountNumber);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(dtoMapper.toBankAccountResponse(testBankAccount)).thenReturn(bankAccountResponse);

        // When
        BankAccountResponse response = accountService.createAccount(createBankAccountRequest, testUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isEqualTo(testAccountNumber);
        assertThat(response.getName()).isEqualTo("My Savings Account");
        verify(userRepository, times(1)).findById(testUserId);
        verify(idGenerator, times(1)).generateAccountNumber();
        verify(bankAccountRepository, times(1)).save(any(BankAccount.class));
        verify(dtoMapper, times(1)).toBankAccountResponse(testBankAccount);
    }

    @Test
    @DisplayName("createAccount - User not found")
    void testCreateAccount_UserNotFound() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.createAccount(createBankAccountRequest, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(testUserId);

        verify(userRepository, times(1)).findById(testUserId);
        verify(idGenerator, never()).generateAccountNumber();
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("createAccount - Verify account entity is built correctly")
    void testCreateAccount_VerifyAccountEntityBuiltCorrectly() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(idGenerator.generateAccountNumber()).thenReturn(testAccountNumber);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(dtoMapper.toBankAccountResponse(any(BankAccount.class))).thenReturn(bankAccountResponse);

        // When
        accountService.createAccount(createBankAccountRequest, testUserId);

        // Then - Verify account entity is saved with correct values
        verify(bankAccountRepository, times(1)).save(argThat(account ->
                testAccountNumber.equals(account.getAccountNumber()) &&
                ApiConstants.DEFAULT_SORT_CODE.equals(account.getSortCode()) &&
                "My Savings Account".equals(account.getName()) &&
                BankAccount.AccountType.personal == account.getAccountType() &&
                BigDecimal.ZERO.equals(account.getBalance()) &&
                BankAccount.Currency.GBP == account.getCurrency() &&
                testUser.equals(account.getUser())
        ));
    }

    @Test
    @DisplayName("createAccount - Verify account number is generated")
    void testCreateAccount_VerifyAccountNumberGenerated() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(idGenerator.generateAccountNumber()).thenReturn(testAccountNumber);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(dtoMapper.toBankAccountResponse(any(BankAccount.class))).thenReturn(bankAccountResponse);

        // When
        accountService.createAccount(createBankAccountRequest, testUserId);

        // Then - Verify account number generator is called
        verify(idGenerator, times(1)).generateAccountNumber();
        verify(bankAccountRepository, times(1)).save(argThat(account ->
                testAccountNumber.equals(account.getAccountNumber())
        ));
    }

    // --- listAccounts Tests ---

    @Test
    @DisplayName("listAccounts - Success with accounts")
    void testListAccounts_Success() {
        // Given
        List<BankAccount> accounts = List.of(testBankAccount);
        List<BankAccountResponse> accountResponses = List.of(bankAccountResponse);
        when(bankAccountRepository.findByUser_Id(testUserId)).thenReturn(accounts);
        when(dtoMapper.toBankAccountResponseList(accounts)).thenReturn(accountResponses);

        // When
        ListBankAccountsResponse response = accountService.listAccounts(testUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccounts()).isNotNull();
        assertThat(response.getAccounts()).hasSize(1);
        assertThat(response.getAccounts().get(0).getAccountNumber()).isEqualTo(testAccountNumber);
        verify(bankAccountRepository, times(1)).findByUser_Id(testUserId);
        verify(dtoMapper, times(1)).toBankAccountResponseList(accounts);
    }

    @Test
    @DisplayName("listAccounts - Success with empty list")
    void testListAccounts_SuccessEmptyList() {
        // Given
        List<BankAccount> accounts = List.of();
        List<BankAccountResponse> accountResponses = List.of();
        when(bankAccountRepository.findByUser_Id(testUserId)).thenReturn(accounts);
        when(dtoMapper.toBankAccountResponseList(accounts)).thenReturn(accountResponses);

        // When
        ListBankAccountsResponse response = accountService.listAccounts(testUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccounts()).isNotNull();
        assertThat(response.getAccounts()).isEmpty();
        verify(bankAccountRepository, times(1)).findByUser_Id(testUserId);
        verify(dtoMapper, times(1)).toBankAccountResponseList(accounts);
    }

    @Test
    @DisplayName("listAccounts - Verify service is called with correct userId")
    void testListAccounts_VerifyServiceCalledWithCorrectUserId() {
        // Given
        when(bankAccountRepository.findByUser_Id(testUserId)).thenReturn(List.of());
        when(dtoMapper.toBankAccountResponseList(any())).thenReturn(List.of());

        // When
        accountService.listAccounts(testUserId);

        // Then - Verify repository was called with correct userId
        verify(bankAccountRepository, times(1)).findByUser_Id(eq(testUserId));
    }

    // --- getAccountByAccountNumber Tests ---

    @Test
    @DisplayName("getAccountByAccountNumber - Success")
    void testGetAccountByAccountNumber_Success() {
        // Given
        when(bankAccountRepository.findByAccountNumber(testAccountNumber)).thenReturn(Optional.of(testBankAccount));
        when(dtoMapper.toBankAccountResponse(testBankAccount)).thenReturn(bankAccountResponse);

        // When
        BankAccountResponse response = accountService.getAccountByAccountNumber(testAccountNumber, testUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isEqualTo(testAccountNumber);
        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(dtoMapper, times(1)).toBankAccountResponse(testBankAccount);
    }

    @Test
    @DisplayName("getAccountByAccountNumber - Account not found")
    void testGetAccountByAccountNumber_AccountNotFound() {
        // Given
        when(bankAccountRepository.findByAccountNumber(testAccountNumber)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getAccountByAccountNumber(testAccountNumber, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bank account")
                .hasMessageContaining(testAccountNumber);

        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(dtoMapper, never()).toBankAccountResponse(any(BankAccount.class));
    }

    @Test
    @DisplayName("getAccountByAccountNumber - Unauthorized (user does not own account)")
    void testGetAccountByAccountNumber_Unauthorized() {
        // Given
        User otherUser = new User();
        otherUser.setId(otherUserId);
        BankAccount otherUserAccount = new BankAccount();
        otherUserAccount.setAccountNumber(testAccountNumber);
        otherUserAccount.setUser(otherUser);

        when(bankAccountRepository.findByAccountNumber(testAccountNumber)).thenReturn(Optional.of(otherUserAccount));

        // When & Then - Should throw UnauthorizedException via AuthorizationHelper
        assertThatThrownBy(() -> accountService.getAccountByAccountNumber(testAccountNumber, testUserId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("bank account");

        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(dtoMapper, never()).toBankAccountResponse(any(BankAccount.class));
    }

    // --- updateAccount Tests ---

    @Test
    @DisplayName("updateAccount - Success with all fields")
    void testUpdateAccount_Success() {
        // Given
        BankAccount updatedAccount = new BankAccount();
        updatedAccount.setAccountNumber(testAccountNumber);
        updatedAccount.setName("My Updated Account");
        updatedAccount.setAccountType(BankAccount.AccountType.personal);

        BankAccountResponse updatedResponse = new BankAccountResponse();
        updatedResponse.setAccountNumber(testAccountNumber);
        updatedResponse.setName("My Updated Account");

        when(bankAccountRepository.findByAccountNumber(testAccountNumber)).thenReturn(Optional.of(testBankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(updatedAccount);
        when(dtoMapper.toBankAccountResponse(updatedAccount)).thenReturn(updatedResponse);

        // When
        BankAccountResponse response = accountService.updateAccount(testAccountNumber, updateBankAccountRequest, testUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("My Updated Account");
        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(bankAccountRepository, times(1)).save(any(BankAccount.class));
        verify(dtoMapper, times(1)).toBankAccountResponse(updatedAccount);
    }

    @Test
    @DisplayName("updateAccount - Account not found")
    void testUpdateAccount_AccountNotFound() {
        // Given
        when(bankAccountRepository.findByAccountNumber(testAccountNumber)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.updateAccount(testAccountNumber, updateBankAccountRequest, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bank account")
                .hasMessageContaining(testAccountNumber);

        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("updateAccount - Unauthorized (user does not own account)")
    void testUpdateAccount_Unauthorized() {
        // Given
        User otherUser = new User();
        otherUser.setId(otherUserId);
        BankAccount otherUserAccount = new BankAccount();
        otherUserAccount.setAccountNumber(testAccountNumber);
        otherUserAccount.setUser(otherUser);

        when(bankAccountRepository.findByAccountNumber(testAccountNumber)).thenReturn(Optional.of(otherUserAccount));

        // When & Then - Should throw UnauthorizedException via AuthorizationHelper
        assertThatThrownBy(() -> accountService.updateAccount(testAccountNumber, updateBankAccountRequest, testUserId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("bank account");

        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("updateAccount - Partial update (only name)")
    void testUpdateAccount_PartialUpdate() {
        // Given
        UpdateBankAccountRequest partialRequest = new UpdateBankAccountRequest();
        partialRequest.setName("My Updated Account");

        when(bankAccountRepository.findByAccountNumber(testAccountNumber)).thenReturn(Optional.of(testBankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(dtoMapper.toBankAccountResponse(any(BankAccount.class))).thenReturn(bankAccountResponse);

        // When
        accountService.updateAccount(testAccountNumber, partialRequest, testUserId);

        // Then - Verify only name is updated, account type unchanged
        verify(bankAccountRepository, times(1)).save(argThat(account ->
                "My Updated Account".equals(account.getName()) &&
                BankAccount.AccountType.personal == account.getAccountType() // Unchanged
        ));
    }

    @Test
    @DisplayName("updateAccount - Verify account entity is updated correctly")
    void testUpdateAccount_VerifyAccountEntityUpdatedCorrectly() {
        // Given
        when(bankAccountRepository.findByAccountNumber(testAccountNumber)).thenReturn(Optional.of(testBankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testBankAccount);
        when(dtoMapper.toBankAccountResponse(any(BankAccount.class))).thenReturn(bankAccountResponse);

        // When
        accountService.updateAccount(testAccountNumber, updateBankAccountRequest, testUserId);

        // Then - Verify account entity is saved with updated values
        verify(bankAccountRepository, times(1)).save(argThat(account ->
                "My Updated Account".equals(account.getName()) &&
                BankAccount.AccountType.personal == account.getAccountType()
        ));
    }

    // --- deleteAccount Tests ---

    @Test
    @DisplayName("deleteAccount - Success")
    void testDeleteAccount_Success() {
        // Given
        when(bankAccountRepository.findByAccountNumber(testAccountNumber)).thenReturn(Optional.of(testBankAccount));
        doNothing().when(bankAccountRepository).deleteById(testAccountNumber);

        // When
        accountService.deleteAccount(testAccountNumber, testUserId);

        // Then
        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(bankAccountRepository, times(1)).deleteById(testAccountNumber);
    }

    @Test
    @DisplayName("deleteAccount - Account not found")
    void testDeleteAccount_AccountNotFound() {
        // Given
        when(bankAccountRepository.findByAccountNumber(testAccountNumber)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.deleteAccount(testAccountNumber, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bank account")
                .hasMessageContaining(testAccountNumber);

        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(bankAccountRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("deleteAccount - Unauthorized (user does not own account)")
    void testDeleteAccount_Unauthorized() {
        // Given
        User otherUser = new User();
        otherUser.setId(otherUserId);
        BankAccount otherUserAccount = new BankAccount();
        otherUserAccount.setAccountNumber(testAccountNumber);
        otherUserAccount.setUser(otherUser);

        when(bankAccountRepository.findByAccountNumber(testAccountNumber)).thenReturn(Optional.of(otherUserAccount));

        // When & Then - Should throw UnauthorizedException via AuthorizationHelper
        assertThatThrownBy(() -> accountService.deleteAccount(testAccountNumber, testUserId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("bank account");

        verify(bankAccountRepository, times(1)).findByAccountNumber(testAccountNumber);
        verify(bankAccountRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("deleteAccount - Verify service is called with correct accountNumber")
    void testDeleteAccount_VerifyServiceCalledWithCorrectAccountNumber() {
        // Given
        when(bankAccountRepository.findByAccountNumber(testAccountNumber)).thenReturn(Optional.of(testBankAccount));
        doNothing().when(bankAccountRepository).deleteById(testAccountNumber);

        // When
        accountService.deleteAccount(testAccountNumber, testUserId);

        // Then - Verify repository was called with correct accountNumber
        verify(bankAccountRepository, times(1)).deleteById(eq(testAccountNumber));
    }
}
