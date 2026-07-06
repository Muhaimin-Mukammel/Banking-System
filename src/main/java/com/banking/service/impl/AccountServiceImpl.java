package com.banking.service.impl;

import com.banking.dto.account.AccountResponse;
import com.banking.dto.account.CreateAccountRequest;
import com.banking.dto.account.DepositRequest;
import com.banking.dto.account.TransferRequest;
import com.banking.dto.account.WithdrawRequest;
import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.InvalidAccountOperationException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.exception.UnauthorizedAccessException;
import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.TransactionStatus;
import com.banking.model.TransactionType;
import com.banking.model.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import com.banking.security.SecurityUtils;
import com.banking.service.AccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public AccountServiceImpl(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        User currentUser = getCurrentUserEntity();

        Account account = new Account(generateUniqueAccountNumber(), request.accountType(), currentUser);
        Account saved = accountRepository.save(account);

        return toAccountResponse(saved);
    }

    @Override
    public AccountResponse getAccountById(Long accountId) {
        Account account = getOwnedAccount(accountId);
        return toAccountResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse deposit(Long accountId, DepositRequest request) {
        Account account = getOwnedAccount(accountId);

        account.setBalance(account.getBalance().add(request.amount()));
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                TransactionType.DEPOSIT, request.amount(), null, account);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);

        return toAccountResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse withdraw(Long accountId, WithdrawRequest request) {
        Account account = getOwnedAccount(accountId);

        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for this withdrawal");
        }

        account.setBalance(account.getBalance().subtract(request.amount()));
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                TransactionType.WITHDRAW, request.amount(), account, null);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);

        return toAccountResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse transfer(Long accountId, TransferRequest request) {
        Account sender = getOwnedAccount(accountId);

        if (accountId.equals(request.receiveAccountId())) {
            throw new InvalidAccountOperationException("Cannot transfer to the same account");
        }

        Account receiver = accountRepository.findById(request.receiveAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receiver account not found with id: " + request.receiveAccountId()));

        if (sender.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for this transfer");
        }

        sender.setBalance(sender.getBalance().subtract(request.amount()));
        receiver.setBalance(receiver.getBalance().add(request.amount()));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transaction transaction = new Transaction(
                TransactionType.TRANSFER, request.amount(), sender, receiver);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);

        return toAccountResponse(sender);
    }

    private Account getOwnedAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + accountId));

        User currentUser = getCurrentUserEntity();
        if (!account.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You do not have access to this account");
        }

        return account;
    }

    private User getCurrentUserEntity() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            long candidate = 1_000_000_000L + (long) (random.nextDouble() * 9_000_000_000L);
            accountNumber = String.valueOf(candidate);
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }

    private AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance()
        );
    }
}