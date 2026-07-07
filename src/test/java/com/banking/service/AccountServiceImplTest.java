package com.banking.service;

import com.banking.dto.account.CreateAccountRequest;
import com.banking.dto.account.DepositRequest;
import com.banking.dto.account.TransferRequest;
import com.banking.dto.account.WithdrawRequest;
import com.banking.exception.AccountLockTimeoutException;
import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.InvalidAccountOperationException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.exception.UnauthorizedAccessException;
import com.banking.model.Account;
import com.banking.model.AccountType;
import com.banking.model.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import com.banking.security.SecurityUtils;
import com.banking.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    private AccountServiceImpl accountService;

    private MockedStatic<SecurityUtils> securityUtils;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(accountRepository, transactionRepository, userRepository);

        owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@bank.com");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@bank.com");

        securityUtils = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    private void loginAs(User user) {
        securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(user.getEmail());
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    private Account accountFor(Long id, User user, BigDecimal balance) {
        Account account = new Account("100000000" + id, AccountType.SAVINGS, user);
        account.setId(id);
        account.setBalance(balance);

        account.setVersion(0L);

        return account;
    }

    @Test
    void createGeneratesAccountWithZeroBalance() {
        loginAs(owner);
        when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId(10L);
            return a;
        });

        var response = accountService.create(new CreateAccountRequest(AccountType.SAVINGS));

        assertEquals(BigDecimal.ZERO, response.balance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void getAccountByIdThrowsWhenNotOwner() {
        loginAs(owner);
        Account account = accountFor(5L, otherUser, BigDecimal.TEN);
        when(accountRepository.findById(5L)).thenReturn(Optional.of(account));

        assertThrows(UnauthorizedAccessException.class, () -> accountService.getAccountById(5L));
    }

    @Test
    void getAccountByIdThrowsWhenMissing() {
        loginAs(owner);
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.getAccountById(99L));
    }

    @Test
    void depositIncreasesBalanceUsingLockedRead() {
        loginAs(owner);
        Account account = accountFor(5L, owner, new BigDecimal("100.00"));
        when(accountRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = accountService.deposit(5L, new DepositRequest(new BigDecimal("50.00")));

        assertEquals(new BigDecimal("150.00"), response.balance());
        verify(accountRepository).findByIdForUpdate(5L);
        verify(accountRepository, never()).findById(5L);
        verify(transactionRepository).save(any());
    }

    @Test
    void withdrawThrowsWhenBalanceTooLow() {
        loginAs(owner);
        Account account = accountFor(5L, owner, new BigDecimal("30.00"));
        when(accountRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(account));

        assertThrows(InsufficientBalanceException.class,
                () -> accountService.withdraw(5L, new WithdrawRequest(new BigDecimal("50.00"))));

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdrawSucceedsWhenFundsAreSufficient() {
        loginAs(owner);
        Account account = accountFor(5L, owner, new BigDecimal("100.00"));
        when(accountRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = accountService.withdraw(5L, new WithdrawRequest(new BigDecimal("40.00")));

        assertEquals(new BigDecimal("60.00"), response.balance());
    }

    @Test
    void depositWrapsPessimisticLockFailure() {
        loginAs(owner);
        when(accountRepository.findByIdForUpdate(5L))
                .thenThrow(new PessimisticLockingFailureException("row locked"));

        assertThrows(AccountLockTimeoutException.class,
                () -> accountService.deposit(5L, new DepositRequest(new BigDecimal("10.00"))));
    }

    @Test
    void transferRejectsSameAccount() {
        loginAs(owner);

        assertThrows(InvalidAccountOperationException.class,
                () -> accountService.transfer(5L, new TransferRequest(5L, new BigDecimal("10.00"))));

        verify(accountRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void transferLocksAccountsInAscendingIdOrderRegardlessOfSenderReceiver() {
        loginAs(owner);
        Account sender = accountFor(9L, owner, new BigDecimal("200.00"));
        Account receiver = accountFor(3L, otherUser, new BigDecimal("50.00"));
        when(accountRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(receiver));
        when(accountRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(sender));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.transfer(9L, new TransferRequest(3L, new BigDecimal("25.00")));

        var inOrder = Mockito.inOrder(accountRepository);
        inOrder.verify(accountRepository).findByIdForUpdate(3L);
        inOrder.verify(accountRepository).findByIdForUpdate(9L);
    }

    @Test
    void transferThrowsWhenSenderNotOwnedByCaller() {
        loginAs(owner);
        Account sender = accountFor(3L, otherUser, new BigDecimal("200.00"));
        Account receiver = accountFor(9L, owner, new BigDecimal("50.00"));
        when(accountRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(receiver));

        assertThrows(UnauthorizedAccessException.class,
                () -> accountService.transfer(3L, new TransferRequest(9L, new BigDecimal("10.00"))));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void transferThrowsWhenSenderBalanceTooLow() {
        loginAs(owner);
        Account sender = accountFor(3L, owner, new BigDecimal("10.00"));
        Account receiver = accountFor(9L, otherUser, new BigDecimal("50.00"));
        when(accountRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(receiver));

        assertThrows(InsufficientBalanceException.class,
                () -> accountService.transfer(3L, new TransferRequest(9L, new BigDecimal("40.00"))));
    }

    @Test
    void transferMovesFundsBetweenBothAccounts() {
        loginAs(owner);
        Account sender = accountFor(3L, owner, new BigDecimal("100.00"));
        Account receiver = accountFor(9L, otherUser, new BigDecimal("50.00"));
        when(accountRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(receiver));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = accountService.transfer(3L, new TransferRequest(9L, new BigDecimal("30.00")));

        assertEquals(new BigDecimal("70.00"), response.balance());
        assertEquals(new BigDecimal("80.00"), receiver.getBalance());
        verify(transactionRepository).save(any());
    }
}
