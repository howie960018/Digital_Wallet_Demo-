package com.wallet.digitalwallet.service;

import com.wallet.digitalwallet.dto.LoginRequest;
import com.wallet.digitalwallet.dto.LoginResponse;
import com.wallet.digitalwallet.entity.User;
import com.wallet.digitalwallet.exception.BusinessException;
import com.wallet.digitalwallet.repository.UserRepository;
import com.wallet.digitalwallet.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {

        // 1. 查使用者
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("AUTH_FAILED", "帳號或密碼錯誤"));

        // 2. 驗證密碼
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("AUTH_FAILED", "帳號或密碼錯誤");
        }

        // 3. 產生 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(token)
                .build();
    }
}