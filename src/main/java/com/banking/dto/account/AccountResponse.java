package com.banking.dto.account;


import com.banking.model.AccountType;
import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        String accountNumber,
        AccountType accountType,
        BigDecimal balance
) {}
