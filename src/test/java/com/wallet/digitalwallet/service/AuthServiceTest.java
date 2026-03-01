package com.wallet.digitalwallet.service;

import com.wallet.digitalwallet.dto.LoginRequest;
import com.wallet.digitalwallet.dto.LoginResponse;
import com.wallet.digitalwallet.entity.User;
import com.wallet.digitalwallet.exception.BusinessException;
import com.wallet.digitalwallet.repository.UserRepository;
import com.wallet.digitalwallet.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 單元測試")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .build();
    }

    @Test
    @DisplayName("登入成功：帳號密碼正確應回傳 Token")
    void login_success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", mockUser.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(1L, "testuser")).thenReturn("mock.jwt.token");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getToken()).isEqualTo("mock.jwt.token");

        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("password123", mockUser.getPasswordHash());
        verify(jwtUtil).generateToken(1L, "testuser");
    }

    @Test
    @DisplayName("登入失敗：帳號不存在應拋出 BusinessException")
    void login_userNotFound_throwsBusinessException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password123");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("帳號或密碼錯誤")
                .extracting("code").isEqualTo("AUTH_FAILED");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("登入失敗：密碼錯誤應拋出 BusinessException")
    void login_wrongPassword_throwsBusinessException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongPassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongPassword", mockUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("帳號或密碼錯誤")
                .extracting("code").isEqualTo("AUTH_FAILED");

        verify(jwtUtil, never()).generateToken(any(), any());
    }
}

