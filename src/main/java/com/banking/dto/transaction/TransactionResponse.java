package com.banking.dto.transaction;

import com.banking.model.TransactionStatus;
import com.banking.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        TransactionType transactionType,
        BigDecimal amount,
        String senderAccountNumber,
        String receiverAccountNumber,
        LocalDateTime transactionTime,
        TransactionStatus status
) {}
