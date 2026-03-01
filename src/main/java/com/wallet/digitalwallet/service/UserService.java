package com.wallet.digitalwallet.service;

import com.wallet.digitalwallet.dto.RegisterRequest;
import com.wallet.digitalwallet.dto.RegisterResponse;
import com.wallet.digitalwallet.entity.Account;
import com.wallet.digitalwallet.entity.User;
import com.wallet.digitalwallet.exception.BusinessException;
import com.wallet.digitalwallet.repository.AccountRepository;
import com.wallet.digitalwallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        // 1. 檢查重複
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("USERNAME_TAKEN", "使用者名稱已被使用");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_TAKEN", "Email 已被使用");
        }

        // 2. 建立 User（密碼加密）
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);

        // 3. 自動開戶（建立預設錢包）
        Account account = Account.builder()
                .userId(user.getId())
                .accountType("WALLET")
                .balance(BigDecimal.ZERO)
                .currency("TWD")
                .status("ACTIVE")
                .build();

        account = accountRepository.save(account);

        // 4. 回傳
        return RegisterResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .accountId(account.getId())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .build();
    }
}