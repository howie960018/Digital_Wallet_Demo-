package com.wallet.digitalwallet.service;

import com.wallet.digitalwallet.dto.AccountResponse;
import com.wallet.digitalwallet.dto.AccountStatusRequest;
import com.wallet.digitalwallet.dto.RegisterRequest;
import com.wallet.digitalwallet.dto.RegisterResponse;
import com.wallet.digitalwallet.entity.Account;
import com.wallet.digitalwallet.entity.User;
import com.wallet.digitalwallet.exception.BusinessException;
import com.wallet.digitalwallet.repository.AccountRepository;
import com.wallet.digitalwallet.repository.AuditLogRepository;
import com.wallet.digitalwallet.repository.UserRepository;
import com.wallet.digitalwallet.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 單元測試")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AuditLogRepository auditLogRepository;


    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        securityUtilMock = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    // ========== register ==========

    @Test
    @DisplayName("註冊成功：新使用者應建立帳戶並回傳 RegisterResponse")
    void register_success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        User savedUser = User.builder()
                .id(10L)
                .username("newuser")
                .email("new@example.com")
                .passwordHash("$2a$10$hashed")
                .build();

        Account savedAccount = Account.builder()
                .id(100L)
                .userId(10L)
                .accountType("WALLET")
                .balance(BigDecimal.ZERO)
                .currency("TWD")
                .status("ACTIVE")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        // Act
        RegisterResponse response = userService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getAccountId()).isEqualTo(100L);
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getCurrency()).isEqualTo("TWD");
    }

    @Test
    @DisplayName("註冊失敗：使用者名稱重複應拋出 BusinessException")
    void register_duplicateUsername_throwsException() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("使用者名稱已被使用")
                .extracting("code").isEqualTo("USERNAME_TAKEN");

        verify(userRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("註冊失敗：Email 重複應拋出 BusinessException")
    void register_duplicateEmail_throwsException() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email 已被使用")
                .extracting("code").isEqualTo("EMAIL_TAKEN");

        verify(userRepository, never()).save(any());
    }

    // ========== getAccounts ==========

    @Test
    @DisplayName("查詢帳戶成功：使用者查自己的帳戶應回傳帳戶列表")
    void getAccounts_success() {
        // Arrange
        Long userId = 1L;
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
        when(userRepository.existsById(userId)).thenReturn(true);

        Account account = Account.builder()
                .id(100L)
                .userId(userId)
                .accountType("WALLET")
                .balance(new BigDecimal("500.00"))
                .currency("TWD")
                .status("ACTIVE")
                .build();

        when(accountRepository.findByUserId(userId)).thenReturn(List.of(account));

        // Act
        List<AccountResponse> responses = userService.getAccounts(userId);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getAccountId()).isEqualTo(100L);
        assertThat(responses.get(0).getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(responses.get(0).getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("查詢帳戶失敗：查看他人帳戶應拋出 BusinessException")
    void getAccounts_accessDenied_throwsException() {
        // Arrange
        Long userId = 2L;
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(1L); // 目前登入者是 ID=1

        // Act & Assert
        assertThatThrownBy(() -> userService.getAccounts(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無權查看他人帳戶")
                .extracting("code").isEqualTo("ACCESS_DENIED");
    }

    @Test
    @DisplayName("查詢帳戶失敗：使用者不存在應拋出 BusinessException")
    void getAccounts_userNotFound_throwsException() {
        // Arrange
        Long userId = 99L;
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(99L);
        when(userRepository.existsById(userId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.getAccounts(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("使用者不存在")
                .extracting("code").isEqualTo("USER_NOT_FOUND");
    }

    // ========== freezeAccount ==========

    @Test
    @DisplayName("凍結帳戶成功：ACTIVE 帳戶應可被凍結")
    void freezeAccount_success() {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(1L);

        AccountStatusRequest request = new AccountStatusRequest();
        request.setAccountId(100L);
        request.setReason("可疑交易");

        Account account = Account.builder()
                .id(100L)
                .userId(1L)
                .accountType("WALLET")
                .balance(BigDecimal.ZERO)
                .currency("TWD")
                .status("ACTIVE")
                .build();

        when(accountRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AccountResponse response = userService.freezeAccount(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo("FROZEN");
        verify(accountRepository).save(argThat(a -> "FROZEN".equals(a.getStatus())));
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("凍結帳戶失敗：帳戶已是 FROZEN 應拋出 BusinessException")
    void freezeAccount_alreadyFrozen_throwsException() {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(1L);

        AccountStatusRequest request = new AccountStatusRequest();
        request.setAccountId(100L);
        request.setReason("已凍結");

        Account account = Account.builder()
                .id(100L)
                .userId(1L)
                .status("FROZEN")
                .build();

        when(accountRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(account));

        // Act & Assert
        assertThatThrownBy(() -> userService.freezeAccount(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("帳戶已經是凍結狀態")
                .extracting("code").isEqualTo("ALREADY_FROZEN");
    }

    @Test
    @DisplayName("凍結帳戶失敗：帳戶不存在應拋出 BusinessException")
    void freezeAccount_notFound_throwsException() {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(1L);

        AccountStatusRequest request = new AccountStatusRequest();
        request.setAccountId(999L);
        request.setReason("測試");

        when(accountRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.freezeAccount(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("ACCESS_DENIED");
    }

    // ========== unfreezeAccount ==========

    @Test
    @DisplayName("解凍帳戶成功：FROZEN 帳戶應可被解凍")
    void unfreezeAccount_success() {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(1L);

        AccountStatusRequest request = new AccountStatusRequest();
        request.setAccountId(100L);
        request.setReason("解凍");

        Account account = Account.builder()
                .id(100L)
                .userId(1L)
                .accountType("WALLET")
                .balance(BigDecimal.ZERO)
                .currency("TWD")
                .status("FROZEN")
                .build();

        when(accountRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AccountResponse response = userService.unfreezeAccount(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(accountRepository).save(argThat(a -> "ACTIVE".equals(a.getStatus())));
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("解凍帳戶失敗：帳戶已是 ACTIVE 應拋出 BusinessException")
    void unfreezeAccount_alreadyActive_throwsException() {
        // Arrange
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(1L);

        AccountStatusRequest request = new AccountStatusRequest();
        request.setAccountId(100L);
        request.setReason("已正常");

        Account account = Account.builder()
                .id(100L)
                .userId(1L)
                .status("ACTIVE")
                .build();

        when(accountRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(account));

        // Act & Assert
        assertThatThrownBy(() -> userService.unfreezeAccount(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("帳戶已經是正常狀態")
                .extracting("code").isEqualTo("ALREADY_ACTIVE");
    }
}

