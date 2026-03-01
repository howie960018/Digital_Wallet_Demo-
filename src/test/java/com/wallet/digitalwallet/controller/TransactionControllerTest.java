package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.TransactionResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("TransactionController 單元測試")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("GET /api/transactions/{accountId} - 查詢交易成功應回傳 200 與分頁資料")
    void getTransactions_success_returns200() throws Exception {
        // Arrange
        Long accountId = 1L;

        TransactionResponse txnResponse = TransactionResponse.builder()
                .txnNo("123456789")
                .type("TRANSFER")
                .amount(new BigDecimal("100.00"))
                .status("SUCCESS")
                .fromAccountId(1L)
                .toAccountId(2L)
                .balanceAfterTxn(new BigDecimal("400.00"))
                .createdAt(LocalDateTime.now())
                .build();

        Page<TransactionResponse> page = new PageImpl<>(
                List.of(txnResponse),
                PageRequest.of(0, 10),
                1
        );

        when(transferService.getTransactions(eq(accountId), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/transactions/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].txnNo").value("123456789"))
                .andExpect(jsonPath("$.data.content[0].type").value("TRANSFER"))
                .andExpect(jsonPath("$.data.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/transactions/{accountId} - 無權操作帳戶應回傳 400")
    void getTransactions_accessDenied_returns400() throws Exception {
        // Arrange
        when(transferService.getTransactions(eq(99L), any()))
                .thenThrow(new BusinessException("ACCESS_DENIED", "無權操作此帳戶"));

        // Act & Assert
        mockMvc.perform(get("/api/transactions/{accountId}", 99L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("無權操作此帳戶"));
    }

    @Test
    @DisplayName("GET /api/transactions/{accountId} - 帳戶無交易紀錄應回傳空頁面")
    void getTransactions_emptyResult_returns200WithEmptyPage() throws Exception {
        // Arrange
        Long accountId = 1L;
        Page<TransactionResponse> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10),
                0
        );

        when(transferService.getTransactions(eq(accountId), any())).thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get("/api/transactions/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}
