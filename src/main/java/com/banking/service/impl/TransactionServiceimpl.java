package com.banking.service.impl;

import com.banking.dto.transaction.TransactionResponse;
import com.banking.exception.ResourceNotFoundException;
import com.banking.exception.UnauthorizedAccessException;
import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import com.banking.security.SecurityUtils;
import com.banking.service.TransactionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionServiceimpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public TransactionServiceimpl(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @Override
    public TransactionResponse getTransactionById(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id: " + transactionId));

        User currentUser = getCurrentUserEntity();
        boolean isSender = transaction.getSenderAccount() != null
                && transaction.getSenderAccount().getUser().getId().equals(currentUser.getId());
        boolean isReceiver = transaction.getReceiverAccount() != null
                && transaction.getReceiverAccount().getUser().getId().equals(currentUser.getId());

        if (!isSender && !isReceiver) {
            throw new UnauthorizedAccessException("You do not have access to this transaction");
        }

        return toTransactionResponse(transaction);
    }

    @Override
    public List<TransactionResponse> getTransactionForAcc(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + accountId));

        User currentUser = getCurrentUserEntity();
        if (!account.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You do not have access to this account");
        }

        return transactionRepository
                .findBySenderAccount_IdOrReceiverAccount_IdOrderByTransactionTimeDesc(accountId, accountId)
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    private User getCurrentUserEntity() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getSenderAccount() != null
                        ? transaction.getSenderAccount().getAccountNumber() : null,
                transaction.getReceiverAccount() != null
                        ? transaction.getReceiverAccount().getAccountNumber() : null,
                transaction.getTransactionTime(),
                transaction.getStatus()
        );
    }
}