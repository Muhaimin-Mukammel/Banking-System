package com.banking.service;

import com.banking.dto.transaction.TransactionResponse;
import com.banking.exception.ResourceNotFoundException;
import com.banking.exception.UnauthorizedAccessException;
import com.banking.model.Account;
import com.banking.model.AccountType;
import com.banking.model.Transaction;
import com.banking.model.TransactionStatus;
import com.banking.model.TransactionType;
import com.banking.model.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import com.banking.security.SecurityUtils;
import com.banking.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    private TransactionServiceImpl transactionService;

    private MockedStatic<SecurityUtils> securityUtils;

    private User caller;
    private User stranger;
    private Account callerAccount;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(transactionRepository, accountRepository, userRepository);

        caller = new User();
        caller.setId(1L);
        caller.setEmail("caller@bank.com");

        stranger = new User();
        stranger.setId(2L);
        stranger.setEmail("stranger@bank.com");

        callerAccount = new Account("1000000001", AccountType.SAVINGS, caller);
        callerAccount.setId(11L);

        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(caller.getEmail());
        lenient().when(userRepository.findByEmail(caller.getEmail())).thenReturn(Optional.of(caller));
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    private Transaction transactionBetween(Long id, Account sender, Account receiver) {
        Transaction transaction = new Transaction(TransactionType.TRANSFER, new BigDecimal("25.00"), sender, receiver);
        transaction.setId(id);
        transaction.setStatus(TransactionStatus.SUCCESS);
        return transaction;
    }

    @Test
    void getAllTransactionsMapsEveryRecord() {
        Transaction t1 = transactionBetween(1L, callerAccount, null);
        Transaction t2 = transactionBetween(2L, null, callerAccount);

        when(transactionRepository.findBySenderAccountUserEmailOrReceiverAccountUserEmail(
                anyString(), anyString()))
                .thenReturn(List.of(t1, t2));

        List<TransactionResponse> responses = transactionService.getAllTransactions();

        verify(transactionRepository).findBySenderAccountUserEmailOrReceiverAccountUserEmail(
                anyString(), anyString());

        assertEquals(2, responses.size());
    }

    @Test
    void getTransactionByIdThrowsWhenNotFound() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.getTransactionById(99L));
    }

    @Test
    void getTransactionByIdAllowsSender() {
        Transaction transaction = transactionBetween(4L, callerAccount, null);
        when(transactionRepository.findById(4L)).thenReturn(Optional.of(transaction));

        var response = transactionService.getTransactionById(4L);

        assertEquals(4L, response.id());
    }

    @Test
    void getTransactionByIdAllowsReceiver() {
        Transaction transaction = transactionBetween(4L, null, callerAccount);
        when(transactionRepository.findById(4L)).thenReturn(Optional.of(transaction));

        var response = transactionService.getTransactionById(4L);

        assertEquals(4L, response.id());
    }

    @Test
    void getTransactionByIdThrowsWhenCallerIsNeitherParty() {
        Account senderAccount = new Account("2000000002", AccountType.SAVINGS, stranger);
        senderAccount.setId(20L);
        Account receiverAccount = new Account("2000000003", AccountType.SAVINGS, stranger);
        receiverAccount.setId(21L);
        Transaction transaction = transactionBetween(4L, senderAccount, receiverAccount);
        when(transactionRepository.findById(4L)).thenReturn(Optional.of(transaction));

        assertThrows(UnauthorizedAccessException.class, () -> transactionService.getTransactionById(4L));
    }

    @Test
    void getTransactionForAccThrowsWhenAccountNotOwned() {
        Account strangerAccount = new Account("3000000004", AccountType.SAVINGS, stranger);
        strangerAccount.setId(30L);
        when(accountRepository.findById(30L)).thenReturn(Optional.of(strangerAccount));

        assertThrows(UnauthorizedAccessException.class, () -> transactionService.getTransactionForAcc(30L));
    }

    @Test
    void getTransactionForAccReturnsOrderedHistory() {
        when(accountRepository.findById(11L)).thenReturn(Optional.of(callerAccount));
        Transaction transaction = transactionBetween(7L, callerAccount, null);
        when(transactionRepository.findBySenderAccount_IdOrReceiverAccount_IdOrderByTransactionTimeDesc(11L, 11L))
                .thenReturn(List.of(transaction));

        var responses = transactionService.getTransactionForAcc(11L);

        assertEquals(1, responses.size());
        assertEquals(7L, responses.get(0).id());
    }

    @Test
    void getTransactionForAccThrowsWhenAccountMissing() {
        when(accountRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.getTransactionForAcc(404L));
    }
}