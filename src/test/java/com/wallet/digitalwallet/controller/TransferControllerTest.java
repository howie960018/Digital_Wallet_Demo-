package com.wallet.digitalwallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.digitalwallet.dto.ApiResponse;
import com.wallet.digitalwallet.dto.TransferRequest;
import com.wallet.digitalwallet.entity.Transaction;
import com.wallet.digitalwallet.exception.BusinessException;
import com.wallet.digitalwallet.exception.GlobalExceptionHandler;
import com.wallet.digitalwallet.service.TransferService;
import com.wallet.digitalwallet.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TransferController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("TransferController 單元測試")
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("POST /api/transfer - 轉帳成功應回傳 200")
    void transfer_success_returns200() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey("key-001");

        Transaction txn = Transaction.builder()
                .id(1L)
                .txnNo("123456789")
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .type("TRANSFER")
                .status("SUCCESS")
                .build();

        when(transferService.transfer(any(TransferRequest.class))).thenReturn(txn);

        // Act & Assert
        mockMvc.perform(post("/api/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("轉帳成功"))
                .andExpect(jsonPath("$.data.txnNo").value("123456789"))
                .andExpect(jsonPath("$.data.type").value("TRANSFER"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /api/transfer - 餘額不足應回傳 400")
    void transfer_insufficientBalance_returns400() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("9999.00"));
        request.setIdempotencyKey("key-002");

        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new BusinessException("INSUFFICIENT_BALANCE", "餘額不足"));

        // Act & Assert
        mockMvc.perform(post("/api/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.message").value("餘額不足"));
    }

    @Test
    @DisplayName("POST /api/transfer - 缺少必填欄位應回傳 400 驗證錯誤")
    void transfer_missingFields_returns400() throws Exception {
        // Arrange: 缺少 idempotencyKey
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        // idempotencyKey 為 null

        // Act & Assert
        mockMvc.perform(post("/api/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/transfer - 帳戶被凍結應回傳 400")
    void transfer_accountFrozen_returns400() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey("key-003");

        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new BusinessException("ACCOUNT_FROZEN", "發送方帳戶已被凍結，無法轉出"));

        // Act & Assert
        mockMvc.perform(post("/api/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACCOUNT_FROZEN"));
    }
}
