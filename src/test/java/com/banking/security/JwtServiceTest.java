package com.banking.security;

import com.banking.annotation.ratelimit.RateLimitInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@WebMvcTest(JwtService.class)
@AutoConfigureMockMvc(addFilters = false)
class JwtServiceTest {

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    private static final String TEST_EMAIL = "test.user@banking.com";
    private static final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxMjM0NSIsIm5hbWUiOiJKb2huIERvZSIsImFkbWluIjp0cnVlLCJleHAiOjE4MTI0NjA4MDB9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @BeforeEach
    void setUp() throws Exception {
        reset(jwtService);
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void generateToken_shouldReturnToken() {
        String expectedToken = VALID_TOKEN;
        when(jwtService.generateToken(TEST_EMAIL)).thenReturn(expectedToken);
        String token = jwtService.generateToken(TEST_EMAIL);
        assertNotNull(token);
        assertEquals(expectedToken, token);
        verify(jwtService, times(1)).generateToken(TEST_EMAIL);
    }

    @Test
    void extractEmail_shouldReturnEmail() {
        when(jwtService.extractEmail(VALID_TOKEN)).thenReturn(TEST_EMAIL);
        String email = jwtService.extractEmail(VALID_TOKEN);
        assertEquals(TEST_EMAIL, email);
        verify(jwtService, times(1)).extractEmail(VALID_TOKEN);
    }

    @Test
    void extractEmail_shouldReturnNullForInvalidToken() {
        String invalidToken = "invalid.token.here";
        when(jwtService.extractEmail(invalidToken)).thenReturn(null);
        String email = jwtService.extractEmail(invalidToken);
        assertNull(email);
    }

    @Test
    void extractEmail_shouldReturnNullForNullToken() {
        when(jwtService.extractEmail(null)).thenReturn(null);
        String email = jwtService.extractEmail(null);
        assertNull(email);
    }
}