package com.banking.dto.user;
public record UserResponse(
        Long id,
        String fullName,
        String email
) {}