package com.banking.repository;

import com.banking.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySenderAccount_IdOrReceiverAccount_IdOrderByTransactionTimeDesc(
            Long senderAccountId, Long receiverAccountId);
    List<Transaction> findBySenderAccountUserEmailOrReceiverAccountUserEmail(
            String senderEmail,
            String receiverEmail);
}
