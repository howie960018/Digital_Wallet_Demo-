package com.wallet.digitalwallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.digitalwallet.dto.*;
import com.wallet.digitalwallet.exception.BusinessException;
import com.wallet.digitalwallet.exception.GlobalExceptionHandler;
import com.wallet.digitalwallet.service.UserService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("UserController 單元測試")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("POST /api/users/register - 註冊成功應回傳 200")
    void register_success_returns200() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        RegisterResponse response = RegisterResponse.builder()
                .userId(1L)
                .username("newuser")
                .email("new@example.com")
                .accountId(100L)
                .balance(BigDecimal.ZERO)
                .currency("TWD")
                .build();

        when(userService.register(any(RegisterRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("newuser"))
                .andExpect(jsonPath("$.data.email").value("new@example.com"))
                .andExpect(jsonPath("$.data.accountId").value(100))
                .andExpect(jsonPath("$.data.currency").value("TWD"));
    }

    @Test
    @DisplayName("POST /api/users/register - 使用者名稱重複應回傳 400")
    void register_duplicateUsername_returns400() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new BusinessException("USERNAME_TAKEN", "使用者名稱已被使用"));

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"))
                .andExpect(jsonPath("$.message").value("使用者名稱已被使用"));
    }

    @Test
    @DisplayName("POST /api/users/register - 密碼太短應回傳 400 驗證錯誤")
    void register_shortPassword_returns400() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("123"); // 太短

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/users/{userId}/accounts - 查詢帳戶成功應回傳 200")
    void getAccounts_success_returns200() throws Exception {
        // Arrange
        Long userId = 1L;
        AccountResponse accountResponse = AccountResponse.builder()
                .accountId(100L)
                .accountType("WALLET")
                .balance(new BigDecimal("500.00"))
                .currency("TWD")
                .status("ACTIVE")
                .build();

        when(userService.getAccounts(eq(userId))).thenReturn(List.of(accountResponse));

        // Act & Assert
        mockMvc.perform(get("/api/users/{userId}/accounts", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].accountId").value(100))
                .andExpect(jsonPath("$.data[0].balance").value(500.00))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/users/{userId}/accounts - 無權查看他人帳戶應回傳 400")
    void getAccounts_accessDenied_returns400() throws Exception {
        // Arrange
        when(userService.getAccounts(eq(2L)))
                .thenThrow(new BusinessException("ACCESS_DENIED", "無權查看他人帳戶"));

        // Act & Assert
        mockMvc.perform(get("/api/users/{userId}/accounts", 2L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("POST /api/users/accounts/freeze - 凍結帳戶成功應回傳 200")
    void freeze_success_returns200() throws Exception {
        // Arrange
        AccountStatusRequest request = new AccountStatusRequest();
        request.setAccountId(100L);
        request.setReason("可疑交易");

        AccountResponse accountResponse = AccountResponse.builder()
                .accountId(100L)
                .accountType("WALLET")
                .balance(BigDecimal.ZERO)
                .currency("TWD")
                .status("FROZEN")
                .build();

        when(userService.freezeAccount(any(AccountStatusRequest.class))).thenReturn(accountResponse);

        // Act & Assert
        mockMvc.perform(post("/api/users/accounts/freeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("帳戶已凍結"))
                .andExpect(jsonPath("$.data.status").value("FROZEN"));
    }

    @Test
    @DisplayName("POST /api/users/accounts/unfreeze - 解凍帳戶成功應回傳 200")
    void unfreeze_success_returns200() throws Exception {
        // Arrange
        AccountStatusRequest request = new AccountStatusRequest();
        request.setAccountId(100L);
        request.setReason("解凍申請");

        AccountResponse accountResponse = AccountResponse.builder()
                .accountId(100L)
                .accountType("WALLET")
                .balance(BigDecimal.ZERO)
                .currency("TWD")
                .status("ACTIVE")
                .build();

        when(userService.unfreezeAccount(any(AccountStatusRequest.class))).thenReturn(accountResponse);

        // Act & Assert
        mockMvc.perform(post("/api/users/accounts/unfreeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("帳戶已解凍"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
}
