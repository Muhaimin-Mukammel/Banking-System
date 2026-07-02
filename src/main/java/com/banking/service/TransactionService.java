package com.banking.service;

import com.banking.dto.transaction.TransactionResponse;

import java.util.List;

public interface TransactionService {
    public List<TransactionResponse> getAllTransactions();

    TransactionResponse getTransactionById(Long transactionId);

    List<TransactionResponse> getTransactionForAcc(Long accountId);
}
