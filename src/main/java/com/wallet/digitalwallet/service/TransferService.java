package com.wallet.digitalwallet.service;

import com.wallet.digitalwallet.dto.TopUpRequest;
import com.wallet.digitalwallet.dto.TransferRequest;
import com.wallet.digitalwallet.dto.TransactionResponse;
import com.wallet.digitalwallet.dto.WithdrawRequest;
import com.wallet.digitalwallet.entity.Account;
import com.wallet.digitalwallet.entity.Transaction;
import com.wallet.digitalwallet.event.TransactionEvent;
import com.wallet.digitalwallet.exception.BusinessException;
import com.wallet.digitalwallet.repository.AccountRepository;
import com.wallet.digitalwallet.repository.TransactionRepository;
import com.wallet.digitalwallet.util.SecurityUtil;
import com.wallet.digitalwallet.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Transaction transfer(TransferRequest request) {

        // 1. 冪等性檢查
        var existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. 權限檢查：只能從自己的帳戶轉出
        verifyAccountOwnership(request.getFromAccountId());

        // 3. 按 id 順序加鎖（防止死鎖）
        Long firstId = Math.min(request.getFromAccountId(), request.getToAccountId());
        Long secondId = Math.max(request.getFromAccountId(), request.getToAccountId());

        Account firstAccount = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "帳戶不存在"));

        Account secondAccount = firstId.equals(secondId)
                ? firstAccount
                : accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "帳戶不存在"));

        Account fromAccount = firstId.equals(request.getFromAccountId()) ? firstAccount : secondAccount;
        Account toAccount = firstId.equals(request.getToAccountId()) ? firstAccount : secondAccount;

        // 4. 業務驗證
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new BusinessException("SELF_TRANSFER", "不能轉帳給自己");
        }

        if ("FROZEN".equals(fromAccount.getStatus())) {
            throw new BusinessException("ACCOUNT_FROZEN", "發送方帳戶已被凍結，無法轉出");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "餘額不足");
        }

        // 5. 扣款與入帳
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        // 6. 建立交易紀錄
        Transaction txn = Transaction.builder()
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

        // 7. 儲存
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        Transaction savedTxn = transactionRepository.save(txn);

        // 8. 發布交易事件（非同步通知）
        publishEvent(savedTxn);
        return savedTxn;
    }

    @Transactional
    public Transaction topUp(TopUpRequest request) {

        // 1. 冪等性檢查
        var existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. 權限檢查：只能儲值到自己的帳戶
        verifyAccountOwnership(request.getAccountId());

        // 3. 查詢帳戶
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "帳戶不存在"));

        // 4. 加錢
        account.setBalance(account.getBalance().add(request.getAmount()));

        // 5. 建立交易紀錄
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

        // 6. 儲存
        accountRepository.save(account);
        Transaction savedTxn = transactionRepository.save(txn);

        // 7. 發布交易事件（非同步通知）
        publishEvent(savedTxn);
        return savedTxn;
    }

    @Transactional
    public Transaction withdraw(WithdrawRequest request) {

        // 1. 冪等性檢查
        var existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. 權限檢查：只能從自己的帳戶提現
        verifyAccountOwnership(request.getAccountId());

        // 3. 查詢帳戶（悲觀鎖）
        Account account = accountRepository.findByIdForUpdate(request.getAccountId())
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "帳戶不存在"));

        // 4. 業務驗證
        if ("FROZEN".equals(account.getStatus())) {
            throw new BusinessException("ACCOUNT_FROZEN", "帳戶已被凍結，無法提現");
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "餘額不足");
        }

        // 5. 扣錢
        account.setBalance(account.getBalance().subtract(request.getAmount()));

        // 6. 建立交易紀錄
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

        // 7. 儲存
        accountRepository.save(account);
        Transaction savedTxn = transactionRepository.save(txn);

        // 8. 發布交易事件（非同步通知）
        publishEvent(savedTxn);
        return savedTxn;
    }

    public Page<TransactionResponse> getTransactions(Long accountId, Pageable pageable) {

        // 權限檢查：只能查自己的交易紀錄
        verifyAccountOwnership(accountId);

        Page<Transaction> transactions = transactionRepository
                .findByFromAccountIdOrToAccountId(accountId, accountId, pageable);

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

    private void verifyAccountOwnership(Long accountId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        accountRepository.findByIdAndUserId(accountId, currentUserId)
                .orElseThrow(() -> new BusinessException("ACCESS_DENIED", "無權操作此帳戶"));
    }

    private void publishEvent(Transaction txn) {
        eventPublisher.publishEvent(new TransactionEvent(
                this,
                txn.getTxnNo(),
                txn.getType(),
                txn.getAmount(),
                txn.getFromAccountId(),
                txn.getToAccountId(),
                txn.getStatus(),
                txn.getRemark()
        ));
    }
}