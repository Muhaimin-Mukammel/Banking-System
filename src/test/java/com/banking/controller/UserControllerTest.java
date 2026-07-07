package com.banking.controller;

import com.banking.annotation.ratelimit.RateLimitInterceptor;
import com.banking.dto.auth.LoginRequest;
import com.banking.dto.auth.LoginResponse;
import com.banking.dto.auth.RegisterRequest;
import com.banking.dto.user.ChangePasswordRequest;
import com.banking.dto.user.UpdateProfileRequest;
import com.banking.dto.user.UserResponse;
import com.banking.security.JwtAuthenticationFilter;
import com.banking.security.JwtService;
import com.banking.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    private UserResponse userResponse;
    private LoginResponse loginResponse;

    // constants
    private final Long USER_ID = 1L;
    private final String FULL_NAME = "John Doe";
    private final String EMAIL = "john@example.com";
    private final String PASSWORD = "password123";
    private final String TOKEN = "jwt-token";
    private final String TOKEN_TYPE = "Bearer";

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        userResponse = new UserResponse(
                USER_ID,
                FULL_NAME,
                EMAIL
        );
        loginResponse = new LoginResponse(
                TOKEN,
                TOKEN_TYPE,
                USER_ID,
                FULL_NAME,
                EMAIL
        );
    }

    @Test
    void register_shouldReturnCreated() throws Exception {
        RegisterRequest request = new RegisterRequest(
                FULL_NAME,
                EMAIL,
                PASSWORD
        );

        when(userService.register(any())).thenReturn(userResponse);

        mockMvc.perform(post("/api/user/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.fullName").value(FULL_NAME))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void login_shouldReturnOk() throws Exception {
        LoginRequest request = new LoginRequest(
                EMAIL,
                PASSWORD
        );

        when(userService.login(any())).thenReturn(loginResponse);

        mockMvc.perform(post("/api/user/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(TOKEN))
                .andExpect(jsonPath("$.tokenType").value(TOKEN_TYPE))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void getProfile_shouldReturnOk() throws Exception {
        when(userService.getCurrentUser()).thenReturn(userResponse);

        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void updateProfile_shouldReturnOk() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(
                FULL_NAME,
                EMAIL
        );

        when(userService.updateProfile(any())).thenReturn(userResponse);

        mockMvc.perform(put("/api/user/me")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value(FULL_NAME))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void changePassword_shouldReturnNoContent() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "oldPass123",
                "newPass1234"
        );

        doNothing().when(userService).changePassword(any());

        mockMvc.perform(put("/api/user/password")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}