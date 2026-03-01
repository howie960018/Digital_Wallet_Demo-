package com.wallet.digitalwallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.digitalwallet.dto.WithdrawRequest;
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

@WebMvcTest(controllers = WithdrawController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("WithdrawController 單元測試")
class WithdrawControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("POST /api/withdraw - 提現成功應回傳 200")
    void withdraw_success_returns200() throws Exception {
        // Arrange
        WithdrawRequest request = new WithdrawRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("200.00"));
        request.setIdempotencyKey("withdraw-001");
        request.setRemark("提現測試");

        Transaction txn = Transaction.builder()
                .id(1L)
                .txnNo("111222333")
                .fromAccountId(1L)
                .amount(new BigDecimal("200.00"))
                .type("WITHDRAW")
                .status("SUCCESS")
                .build();

        when(transferService.withdraw(any(WithdrawRequest.class))).thenReturn(txn);

        // Act & Assert
        mockMvc.perform(post("/api/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("提現成功"))
                .andExpect(jsonPath("$.data.txnNo").value("111222333"))
                .andExpect(jsonPath("$.data.type").value("WITHDRAW"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /api/withdraw - 餘額不足應回傳 400")
    void withdraw_insufficientBalance_returns400() throws Exception {
        // Arrange
        WithdrawRequest request = new WithdrawRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("9999.00"));
        request.setIdempotencyKey("withdraw-002");

        when(transferService.withdraw(any(WithdrawRequest.class)))
                .thenThrow(new BusinessException("INSUFFICIENT_BALANCE", "餘額不足"));

        // Act & Assert
        mockMvc.perform(post("/api/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.message").value("餘額不足"));
    }

    @Test
    @DisplayName("POST /api/withdraw - 帳戶被凍結應回傳 400")
    void withdraw_accountFrozen_returns400() throws Exception {
        // Arrange
        WithdrawRequest request = new WithdrawRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey("withdraw-003");

        when(transferService.withdraw(any(WithdrawRequest.class)))
                .thenThrow(new BusinessException("ACCOUNT_FROZEN", "帳戶已被凍結，無法提現"));

        // Act & Assert
        mockMvc.perform(post("/api/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACCOUNT_FROZEN"));
    }

    @Test
    @DisplayName("POST /api/withdraw - 缺少必填欄位應回傳 400 驗證錯誤")
    void withdraw_missingIdempotencyKey_returns400() throws Exception {
        // Arrange: idempotencyKey 為 null
        WithdrawRequest request = new WithdrawRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("100.00"));

        // Act & Assert
        mockMvc.perform(post("/api/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
