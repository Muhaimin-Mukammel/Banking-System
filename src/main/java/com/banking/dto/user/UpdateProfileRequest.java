package com.banking.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest (
        @NotBlank(message = "Full name is required")
        String fullName,

        @Email(message = "Invalid Email")
        @NotBlank(message = "Email is required")
        String email
){}
