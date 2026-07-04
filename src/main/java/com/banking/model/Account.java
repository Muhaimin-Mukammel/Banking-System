package com.banking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "senderAccount", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Transaction> sentTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "receiverAccount", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Transaction> receivedTransactions = new ArrayList<>();

    public Account(String accountNumber, AccountType accountType, User user) {
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.user = user;
        this.balance = BigDecimal.ZERO;
    }
}