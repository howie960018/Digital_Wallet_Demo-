package com.wallet.digitalwallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.digitalwallet.dto.TopUpRequest;
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

@WebMvcTest(controllers = TopUpController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("TopUpController 單元測試")
class TopUpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("POST /api/topup - 儲值成功應回傳 200")
    void topUp_success_returns200() throws Exception {
        // Arrange
        TopUpRequest request = new TopUpRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("500.00"));
        request.setIdempotencyKey("topup-001");
        request.setRemark("儲值測試");

        Transaction txn = Transaction.builder()
                .id(1L)
                .txnNo("987654321")
                .toAccountId(1L)
                .amount(new BigDecimal("500.00"))
                .type("TOPUP")
                .status("SUCCESS")
                .build();

        when(transferService.topUp(any(TopUpRequest.class))).thenReturn(txn);

        // Act & Assert
        mockMvc.perform(post("/api/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("儲值成功"))
                .andExpect(jsonPath("$.data.txnNo").value("987654321"))
                .andExpect(jsonPath("$.data.type").value("TOPUP"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /api/topup - 帳戶不存在應回傳 400")
    void topUp_accountNotFound_returns400() throws Exception {
        // Arrange
        TopUpRequest request = new TopUpRequest();
        request.setAccountId(99L);
        request.setAmount(new BigDecimal("500.00"));
        request.setIdempotencyKey("topup-002");

        when(transferService.topUp(any(TopUpRequest.class)))
                .thenThrow(new BusinessException("ACCOUNT_NOT_FOUND", "帳戶不存在"));

        // Act & Assert
        mockMvc.perform(post("/api/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("帳戶不存在"));
    }

    @Test
    @DisplayName("POST /api/topup - 金額為零應回傳 400 驗證錯誤")
    void topUp_zeroAmount_returns400() throws Exception {
        // Arrange
        TopUpRequest request = new TopUpRequest();
        request.setAccountId(1L);
        request.setAmount(BigDecimal.ZERO); // 不符合 @DecimalMin("0.0001")
        request.setIdempotencyKey("topup-003");

        // Act & Assert
        mockMvc.perform(post("/api/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
