package com.banking.service.impl;

import com.banking.dto.auth.LoginRequest;
import com.banking.dto.auth.LoginResponse;
import com.banking.dto.auth.RegisterRequest;
import com.banking.dto.user.ChangePasswordRequest;
import com.banking.dto.user.UpdateProfileRequest;
import com.banking.dto.user.UserResponse;
import com.banking.exception.InvalidCredentialsException;
import com.banking.exception.ResourceAlreadyExistsException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.model.User;
import com.banking.repository.UserRepository;
import com.banking.security.JwtService;
import com.banking.security.SecurityUtils;
import com.banking.service.UserService;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResourceAlreadyExistsException(
                    "An account with email " + request.email() + " already exists");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        try {
            User saved = userRepository.save(user);
            return toUserResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException(
                    "An account with email " + request.email() + " already exists");
        }
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail());

        return new LoginResponse(
                token,
                "Bearer",
                user.getId(),
                user.getFullName(),
                user.getEmail()
        );
    }

    @Override
    @Cacheable(value = "userCahce", key = "T(com.banking.security.SecurityUtils).getCurrentUserEmail()")
    public UserResponse getCurrentUser() {
        return toUserResponse(getCurrentUserEntity());
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUserEntity();

        if (!user.getEmail().equals(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new ResourceAlreadyExistsException(
                    "An account with email " + request.email() + " already exists");
        }

        user.setFullName(request.fullName());
        user.setEmail(request.email());

        try {
            User saved = userRepository.save(user);
            return toUserResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceAlreadyExistsException(
                    "An account with email " + request.email() + " already exists");
        }
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUserEntity();

        if (!passwordEncoder.matches(request.currentPass(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPass()));
        userRepository.save(user);
    }

    private User getCurrentUserEntity() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}