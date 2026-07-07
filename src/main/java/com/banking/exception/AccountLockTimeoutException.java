package com.banking.exception;

public class AccountLockTimeoutException extends RuntimeException {
    public AccountLockTimeoutException(String message) {
        super(message);
    }
}
