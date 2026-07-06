package com.banking.controller;

import com.banking.dto.account.*;
import com.banking.model.AccountType;
import com.banking.security.JwtAuthenticationFilter;
import com.banking.security.JwtService;
import com.banking.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    private CreateAccountRequest createAccountRequest;
    private DepositRequest depositRequest;
    private TransferRequest transferRequest;
    private WithdrawRequest withdrawRequest;
    private AccountResponse accountResponse;

    // Constants

    private final BigDecimal BALANCE = BigDecimal.valueOf(100000);
    private final Long RECEIVE_ACCOUNT_NO = Long.valueOf(12345);
    private final String ACCOUNT_NUMBER = "qwert";
    private final Long ID = Long.valueOf(23);
    private final AccountType ACCOUNT_TYPE = AccountType.CURRENT;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(1000);

    @BeforeEach
    void setUp(){
        createAccountRequest = new CreateAccountRequest(ACCOUNT_TYPE);
        depositRequest = new DepositRequest(AMOUNT);
        transferRequest = new TransferRequest(RECEIVE_ACCOUNT_NO, AMOUNT);
        withdrawRequest = new WithdrawRequest(AMOUNT);
        accountResponse = new AccountResponse(ID, ACCOUNT_NUMBER, ACCOUNT_TYPE, BALANCE);
    }

    @Test
    void createAccount_shouldReturnCreated() throws Exception {
        when(accountService.create(createAccountRequest)).thenReturn(accountResponse);

        mockMvc.perform(post("/api/account/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER))
                .andExpect(jsonPath("$.balance").value(BALANCE));
    }

    @Test
    void getAccount_shouldReturnOk() throws Exception {
        when(accountService.getAccountById(ID)).thenReturn(accountResponse);

        mockMvc.perform(get("/api/account/{accountId}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER));
    }

    @Test
    void deposit_shouldReturnOk() throws Exception {
        when(accountService.deposit(ID, depositRequest)).thenReturn(accountResponse);

        mockMvc.perform(post("/api/account/{accountId}/deposit", ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(BALANCE));
    }

    @Test
    void withdraw_shouldReturnOk() throws Exception {
        when(accountService.withdraw(ID, withdrawRequest)).thenReturn(accountResponse);

        mockMvc.perform(post("/api/account/{accountId}/withdraw", ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void transfer_shouldReturnOk() throws Exception {
        when(accountService.transfer(ID, transferRequest)).thenReturn(accountResponse);

        mockMvc.perform(post("/api/account/{accountId}/transfer", ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk());
    }
}
