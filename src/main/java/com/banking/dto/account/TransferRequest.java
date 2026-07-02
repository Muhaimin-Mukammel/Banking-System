package com.banking.dto.account;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest (
        @NotNull(message = "Reciver account ID is required")
        Long receiveAccountId,

        @Positive(message = "Amount must be greater than 0")
        BigDecimal amount
){}
