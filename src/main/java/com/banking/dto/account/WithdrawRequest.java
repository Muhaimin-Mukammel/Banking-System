package com.banking.dto.account;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record WithdrawRequest (
        @Positive(message = "Amount must be greater than 0")
        BigDecimal amount
){
}
