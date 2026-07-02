package com.banking.service;

import com.banking.dto.auth.LoginRequest;
import com.banking.dto.auth.LoginResponse;
import com.banking.dto.auth.RegisterRequest;
import com.banking.dto.user.ChangePasswordRequest;
import com.banking.dto.user.UpdateProfileRequest;
import com.banking.dto.user.UserResponse;

public interface UserService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserResponse getCurrentUser();

    UserResponse updateProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);
}
