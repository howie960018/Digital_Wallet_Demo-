package com.wallet.digitalwallet.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil 單元測試")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // 使用足夠長的 secret（至少 256 bits = 32 bytes）
        String secret = "mySecretKey12345678901234567890AB";
        long expiration = 3600000L; // 1 hour
        jwtUtil = new JwtUtil(secret, expiration);
    }

    @Test
    @DisplayName("產生 Token：應成功建立非空的 JWT")
    void generateToken_success() {
        // Act
        String token = jwtUtil.generateToken(1L, "testuser");

        // Assert
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // JWT 格式：header.payload.signature
    }

    @Test
    @DisplayName("解析 Username：應從 Token 中正確取得使用者名稱")
    void getUsername_success() {
        // Arrange
        String token = jwtUtil.generateToken(1L, "testuser");

        // Act
        String username = jwtUtil.getUsername(token);

        // Assert
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("解析 UserId：應從 Token 中正確取得使用者 ID")
    void getUserId_success() {
        // Arrange
        String token = jwtUtil.generateToken(42L, "testuser");

        // Act
        Long userId = jwtUtil.getUserId(token);

        // Assert
        assertThat(userId).isEqualTo(42L);
    }

    @Test
    @DisplayName("驗證 Token：有效 Token 應回傳 true")
    void validateToken_validToken_returnsTrue() {
        // Arrange
        String token = jwtUtil.generateToken(1L, "testuser");

        // Act
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("驗證 Token：無效 Token 應回傳 false")
    void validateToken_invalidToken_returnsFalse() {
        // Act
        boolean isValid = jwtUtil.validateToken("invalid.token.string");

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("驗證 Token：竄改過的 Token 應回傳 false")
    void validateToken_tamperedToken_returnsFalse() {
        // Arrange
        String token = jwtUtil.generateToken(1L, "testuser");
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        // Act
        boolean isValid = jwtUtil.validateToken(tamperedToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("驗證 Token：已過期 Token 應回傳 false")
    void validateToken_expiredToken_returnsFalse() {
        // Arrange: 使用 -1 ms 過期時間（立即過期）
        JwtUtil expiredJwtUtil = new JwtUtil("mySecretKey12345678901234567890AB", -1000L);
        String token = expiredJwtUtil.generateToken(1L, "testuser");

        // Act
        boolean isValid = expiredJwtUtil.validateToken(token);

        // Assert
        assertThat(isValid).isFalse();
    }
}

