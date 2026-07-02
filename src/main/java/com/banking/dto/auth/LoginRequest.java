package com.banking.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest (
    @Email(message = "Invalid Email")
    @NotBlank(message = "Email is required")
    String email,

    @NotBlank(message =  " Password is Required ")
    Long password
){}

