package com.banking.controller;

import com.banking.dto.transaction.TransactionResponse;
import com.banking.model.TransactionStatus;
import com.banking.model.TransactionType;
import com.banking.security.JwtAuthenticationFilter;
import com.banking.security.JwtService;
import com.banking.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private TransactionResponse transactionResponse;


    // constants
    private final Long TRANSACTION_ID = Long.valueOf(1542);
    private final Long ACCOUNT_ID = Long.valueOf(125);
    private final BigDecimal AMOUNT = BigDecimal.valueOf(1000);
    private final TransactionType TRANSACTION_TYPE = TransactionType.DEPOSIT;
    private final String RECEIVE_ACCOUNT_NUMBER = "ACC-001";
    private final String SEND_ACCOUNT_NUMBER = "ACC-245";
    private final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.now();
    private final TransactionStatus TRANSACTION_STATUS = TransactionStatus.SUCCESS;

    @BeforeEach
    void setUp(){
        transactionResponse = new TransactionResponse(TRANSACTION_ID, TRANSACTION_TYPE, AMOUNT, SEND_ACCOUNT_NUMBER, RECEIVE_ACCOUNT_NUMBER, LOCAL_DATE_TIME, TRANSACTION_STATUS);
    }

    @Test
    void getAllTransaction_shouldReturnOk() throws Exception {
        when(transactionService.getAllTransactions())
                .thenReturn(List.of(transactionResponse));

        mockMvc.perform(get("/api/transaction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TRANSACTION_ID));
    }

    @Test
    void getTransactionById_shouldReturnOk() throws Exception {
        when(transactionService.getTransactionById(TRANSACTION_ID))
                .thenReturn(transactionResponse);

        mockMvc.perform(get("/api/transaction/{transactionId}", TRANSACTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TRANSACTION_ID));
    }

    @Test
    void getTransactionForSpecificAccount_shouldReturnOk() throws Exception {
        when(transactionService.getTransactionForAcc(ACCOUNT_ID))
                .thenReturn(List.of(transactionResponse));

        mockMvc.perform(get("/api/transaction/account/{accountId}", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TRANSACTION_ID));
    }
}
