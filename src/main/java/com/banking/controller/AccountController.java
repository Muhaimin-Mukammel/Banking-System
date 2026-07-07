package com.banking.controller;

import com.banking.annotation.ratelimit.RateLimit;
import com.banking.dto.account.*;
import com.banking.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/create")
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request){
        AccountResponse response = accountService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable Long accountId){
        AccountResponse response = accountService.getAccountById(accountId);
        return ResponseEntity.ok(response);
    }

    @RateLimit(capacity = 20, refillTokens = 20, refillPeriodSeconds = 60)
    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable Long accountId,
            @Valid @RequestBody DepositRequest request){
        AccountResponse response = accountService.deposit(accountId, request);
        return ResponseEntity.ok(response);
    }

    @RateLimit(capacity = 20, refillTokens = 20, refillPeriodSeconds = 60)
    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<AccountResponse> withDraw(
            @PathVariable Long accountId,
            @Valid @RequestBody WithdrawRequest request){
        AccountResponse response = accountService.withdraw(accountId, request);
        return ResponseEntity.ok(response);
    }

    @RateLimit(capacity = 20, refillTokens = 20, refillPeriodSeconds = 60)
    @PostMapping("/{accountId}/transfer")
    public ResponseEntity<AccountResponse> transfer(
            @PathVariable Long accountId,
            @Valid @RequestBody TransferRequest request){
        AccountResponse response = accountService.transfer(accountId, request);
        return ResponseEntity.ok(response);
    }
}
