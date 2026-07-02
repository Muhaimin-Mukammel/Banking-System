package com.banking.dto.account;

import com.banking.model.AccountType;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotNull(message = "Account type is required")
        AccountType accountType
) {}
