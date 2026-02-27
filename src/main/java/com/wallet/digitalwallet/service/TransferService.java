package com.wallet.digitalwallet.service;

import com.wallet.digitalwallet.dto.TransferRequest;
import com.wallet.digitalwallet.entity.Account;
import com.wallet.digitalwallet.entity.Transaction;
import com.wallet.digitalwallet.exception.BusinessException;
import com.wallet.digitalwallet.repository.AccountRepository;
import com.wallet.digitalwallet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction transfer(TransferRequest request) {

        // 1. 冪等性檢查：這個 key 是否已經處理過？
        var existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. 查詢帳戶（悲觀鎖，防止併發）
//        Account fromAccount = accountRepository.findByIdForUpdate(request.getFromAccountId())
//                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "發送方帳戶不存在"));
//
//        Account toAccount = accountRepository.findByIdForUpdate(request.getToAccountId())
//                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "接收方帳戶不存在"));


        // 2. 按 id 順序加鎖（防止死鎖）
        Long firstId = Math.min(request.getFromAccountId(), request.getToAccountId());
        Long secondId = Math.max(request.getFromAccountId(), request.getToAccountId());

        Account firstAccount = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "帳戶不存在"));

        Account secondAccount = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "帳戶不存在"));

        // 根據實際角色分配
        Account fromAccount = firstId.equals(request.getFromAccountId()) ? firstAccount : secondAccount;
        Account toAccount = firstId.equals(request.getToAccountId()) ? firstAccount : secondAccount;

        // 3. 業務驗證
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new BusinessException("SELF_TRANSFER", "不能轉帳給自己");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "餘額不足");
        }

        // 4. 扣款與入帳
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        // 5. 建立交易紀錄
        Transaction txn = Transaction.builder()
                .txnNo(UUID.randomUUID().toString())
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(request.getAmount())
                .fee(BigDecimal.ZERO)
                .type("TRANSFER")
                .status("SUCCESS")
                .idempotencyKey(request.getIdempotencyKey())
                .balanceAfterTxn(fromAccount.getBalance())
                .remark(request.getRemark())
                .build();

        // 6. 儲存
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        return transactionRepository.save(txn);
    }
}