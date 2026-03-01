package com.wallet.digitalwallet.service;

import com.wallet.digitalwallet.dto.RegisterRequest;
import com.wallet.digitalwallet.dto.RegisterResponse;
import com.wallet.digitalwallet.entity.Account;
import com.wallet.digitalwallet.entity.AuditLog;
import com.wallet.digitalwallet.entity.User;
import com.wallet.digitalwallet.exception.BusinessException;
import com.wallet.digitalwallet.repository.AccountRepository;
import com.wallet.digitalwallet.repository.AuditLogRepository;
import com.wallet.digitalwallet.repository.UserRepository;
import com.wallet.digitalwallet.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import com.wallet.digitalwallet.dto.AccountResponse;
import java.util.List;
import java.util.stream.Collectors;
import com.wallet.digitalwallet.dto.AccountStatusRequest;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AuditLogRepository auditLogRepository;
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


    public List<AccountResponse> getAccounts(Long userId) {

        // 新增：只能查自己的帳戶
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new BusinessException("ACCESS_DENIED", "無權查看他人帳戶");
        }

        if (!userRepository.existsById(userId)) {
            throw new BusinessException("USER_NOT_FOUND", "使用者不存在");
        }

        return accountRepository.findByUserId(userId).stream()
                .map(this::toAccountResponse)
                .collect(Collectors.toList());
    }






    @Transactional
    public AccountResponse freezeAccount(AccountStatusRequest request) {

        // 權限檢查：只能凍結自己的帳戶
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), currentUserId)
                .orElseThrow(() -> new BusinessException("ACCESS_DENIED", "無權操作此帳戶"));

        if ("FROZEN".equals(account.getStatus())) {
            throw new BusinessException("ALREADY_FROZEN", "帳戶已經是凍結狀態");
        }

        String oldStatus = account.getStatus();
        account.setStatus("FROZEN");
        accountRepository.save(account);

        // 記錄稽核日誌（Audit Log）
        auditLogRepository.save(AuditLog.builder()
                .accountId(account.getId())
                .userId(currentUserId)
                .action("FREEZE_ACCOUNT")
                .reason(request.getReason())
                .oldStatus(oldStatus)
                .newStatus("FROZEN")
                .build());

        return toAccountResponse(account);
    }

    @Transactional
    public AccountResponse unfreezeAccount(AccountStatusRequest request) {

        // 權限檢查：只能解凍自己的帳戶
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), currentUserId)
                .orElseThrow(() -> new BusinessException("ACCESS_DENIED", "無權操作此帳戶"));

        if ("ACTIVE".equals(account.getStatus())) {
            throw new BusinessException("ALREADY_ACTIVE", "帳戶已經是正常狀態");
        }

        String oldStatus = account.getStatus();
        account.setStatus("ACTIVE");
        accountRepository.save(account);

        // 記錄稽核日誌（Audit Log）
        auditLogRepository.save(AuditLog.builder()
                .accountId(account.getId())
                .userId(currentUserId)
                .action("UNFREEZE_ACCOUNT")
                .reason(request.getReason())
                .oldStatus(oldStatus)
                .newStatus("ACTIVE")
                .build());

        return toAccountResponse(account);
    }

    // 抽出共用的轉換方法，避免重複程式碼
    private AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .accountId(account.getId())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .build();
    }
}