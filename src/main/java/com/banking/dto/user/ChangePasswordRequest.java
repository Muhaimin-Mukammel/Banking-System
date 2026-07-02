package com.banking.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest (
        @NotBlank(message = "Current Password is requeired")
        String currentPass,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password mush be at least 8 characters")
        String newPass
){}
