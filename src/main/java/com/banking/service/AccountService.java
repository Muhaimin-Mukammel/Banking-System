package com.banking.service;

import com.banking.dto.account.*;
import jakarta.validation.Valid;

public interface AccountService {
    AccountResponse create(@Valid CreateAccountRequest request);

    AccountResponse getAccountById(Long accountId);

    AccountResponse deposit(Long accountId, @Valid DepositRequest request);

    AccountResponse withdraw(Long accountId, @Valid WithdrawRequest request);

    AccountResponse transfer(Long accountId, @Valid TransferRequest request);
}
