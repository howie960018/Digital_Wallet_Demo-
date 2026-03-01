package com.wallet.digitalwallet.config;

import com.wallet.digitalwallet.entity.Account;
import com.wallet.digitalwallet.entity.User;
import com.wallet.digitalwallet.repository.AccountRepository;
import com.wallet.digitalwallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (userRepository.count() > 0) {
            log.info("資料已存在，跳過初始化");
            return;
        }

        String[][] users = {
                {"alice",   "alice@example.com",   "password123", "50000"},
                {"bob",     "bob@example.com",     "password123", "30000"},
                {"charlie", "charlie@example.com", "password123", "10000"},
                {"david",   "david@example.com",   "password123", "80000"},
                {"eva",     "eva@example.com",     "password123", "5000"}
        };

        for (String[] data : users) {
            User user = userRepository.save(User.builder()
                    .username(data[0])
                    .email(data[1])
                    .passwordHash(passwordEncoder.encode(data[2]))
                    .build());

            accountRepository.save(Account.builder()
                    .userId(user.getId())
                    .accountType("WALLET")
                    .balance(new BigDecimal(data[3]))
                    .currency("TWD")
                    .status("ACTIVE")
                    .build());

            log.info("建立使用者：{} 餘額：{}", data[0], data[3]);
        }

        log.info("初始化完成，共建立 {} 位使用者", users.length);
    }
}
