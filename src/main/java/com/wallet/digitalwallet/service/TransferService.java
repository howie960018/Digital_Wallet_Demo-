package com.wallet.digitalwallet.service;

import com.wallet.digitalwallet.dto.TopUpRequest;
import com.wallet.digitalwallet.dto.TransferRequest;
import com.wallet.digitalwallet.dto.WithdrawRequest;
import com.wallet.digitalwallet.entity.Account;
import com.wallet.digitalwallet.entity.Transaction;
import com.wallet.digitalwallet.exception.BusinessException;
import com.wallet.digitalwallet.repository.AccountRepository;
import com.wallet.digitalwallet.repository.TransactionRepository;
import com.wallet.digitalwallet.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import com.wallet.digitalwallet.dto.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // 加入欄位
    private final SnowflakeIdGenerator idGenerator;


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

        // 新增：檢查發送方帳戶狀態
        if ("FROZEN".equals(fromAccount.getStatus())) {
            throw new BusinessException("ACCOUNT_FROZEN", "發送方帳戶已被凍結，無法轉出");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "餘額不足");
        }

        // 4. 扣款與入帳
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        // 5. 建立交易紀錄
        Transaction txn = Transaction.builder()
                // 把 builder 裡的 txnNo 改掉
                .txnNo(String.valueOf(idGenerator.nextId()))
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


    @Transactional
    public Transaction topUp(TopUpRequest request) {

        // 1. 冪等性檢查
        var existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. 查詢帳戶（儲值不需要悲觀鎖，因為只有加錢，不會有餘額不足的問題）
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "帳戶不存在"));

        // 3. 加錢
        account.setBalance(account.getBalance().add(request.getAmount()));

        // 4. 建立交易紀錄
        Transaction txn = Transaction.builder()
                .txnNo(String.valueOf(idGenerator.nextId()))
                .fromAccountId(null)
                .toAccountId(account.getId())
                .amount(request.getAmount())
                .fee(BigDecimal.ZERO)
                .type("TOPUP")
                .status("SUCCESS")
                .idempotencyKey(request.getIdempotencyKey())
                .balanceAfterTxn(account.getBalance())
                .remark(request.getRemark())
                .build();

        accountRepository.save(account);
        return transactionRepository.save(txn);
    }


    @Transactional
    public Transaction withdraw(WithdrawRequest request) {

        // 1. 冪等性檢查
        var existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. 查詢帳戶（提現需要悲觀鎖，因為要扣錢）
        Account account = accountRepository.findByIdForUpdate(request.getAccountId())
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "帳戶不存在"));

        // 3. 餘額檢查

        // 3. 餘額檢查
        // 新增：檢查帳戶狀態
        if ("FROZEN".equals(account.getStatus())) {
            throw new BusinessException("ACCOUNT_FROZEN", "帳戶已被凍結，無法提現");
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "餘額不足");
        }

        // 4. 扣錢
        account.setBalance(account.getBalance().subtract(request.getAmount()));

        // 5. 建立交易紀錄
        Transaction txn = Transaction.builder()
                .txnNo(String.valueOf(idGenerator.nextId()))
                .fromAccountId(account.getId())
                .toAccountId(null)
                .amount(request.getAmount())
                .fee(BigDecimal.ZERO)
                .type("WITHDRAW")
                .status("SUCCESS")
                .idempotencyKey(request.getIdempotencyKey())
                .balanceAfterTxn(account.getBalance())
                .remark(request.getRemark())
                .build();

        accountRepository.save(account);
        return transactionRepository.save(txn);
    }


    public Page<TransactionResponse> getTransactions(Long accountId, Pageable pageable) {

        // 1. 確認帳戶存在
        accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "帳戶不存在"));

        // 2. 查詢交易紀錄（轉入 + 轉出）
        Page<Transaction> transactions = transactionRepository
                .findByFromAccountIdOrToAccountId(accountId, accountId, pageable);

        // 3. Entity 轉 DTO
        return transactions.map(txn -> TransactionResponse.builder()
                .txnNo(txn.getTxnNo())
                .type(txn.getType())
                .amount(txn.getAmount())
                .status(txn.getStatus())
                .fromAccountId(txn.getFromAccountId())
                .toAccountId(txn.getToAccountId())
                .balanceAfterTxn(txn.getBalanceAfterTxn())
                .remark(txn.getRemark())
                .createdAt(txn.getCreatedAt())
                .build());
    }
}