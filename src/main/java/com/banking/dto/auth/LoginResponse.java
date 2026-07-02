package com.banking.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginResponse (
        String token,
        String tokenType,
        Long userId,
        String fullName,
        String email
){}
