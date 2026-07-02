package com.banking.dto.auth;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse (
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> validationErrors
){
}
