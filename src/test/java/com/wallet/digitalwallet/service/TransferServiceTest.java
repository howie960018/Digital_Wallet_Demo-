package com.wallet.digitalwallet.service;

import com.wallet.digitalwallet.dto.TopUpRequest;
import com.wallet.digitalwallet.dto.TransferRequest;
import com.wallet.digitalwallet.dto.WithdrawRequest;
import com.wallet.digitalwallet.entity.Account;
import com.wallet.digitalwallet.entity.Transaction;
import com.wallet.digitalwallet.exception.BusinessException;
import com.wallet.digitalwallet.repository.AccountRepository;
import com.wallet.digitalwallet.repository.TransactionRepository;
import com.wallet.digitalwallet.util.SecurityUtil;
import com.wallet.digitalwallet.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService 單元測試")
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SnowflakeIdGenerator idGenerator;
    @Mock
    private ApplicationEventPublisher eventPublisher;


    @InjectMocks
    private TransferService transferService;

    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
        // 注意：idGenerator.nextId() 的 stub 只在真正建立交易的測試中才設定
        // 避免冪等性測試等不需要它的 case 觸發 UnnecessaryStubbingException
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    // ========== transfer ==========

    @Test
    @DisplayName("轉帳成功：正常轉帳應扣款並入帳")
    void transfer_success() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey("key-001");
        request.setRemark("測試轉帳");

        Account fromAccount = Account.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("500.00"))
                .status("ACTIVE")
                .build();

        Account toAccount = Account.builder()
                .id(2L)
                .userId(2L)
                .balance(new BigDecimal("200.00"))
                .status("ACTIVE")
                .build();

        Transaction savedTxn = Transaction.builder()
                .id(1L)
                .txnNo("123456789")
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .type("TRANSFER")
                .status("SUCCESS")
                .build();

        when(transactionRepository.findByIdempotencyKey("key-001")).thenReturn(Optional.empty());
        when(idGenerator.nextId()).thenReturn(123456789L);
        // verifyAccountOwnership: findByIdAndUserId(1L, 1L)
        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(fromAccount));
        // findByIdForUpdate for firstId=1, secondId=2
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTxn);

        // Act
        Transaction result = transferService.transfer(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo("TRANSFER");
        assertThat(fromAccount.getBalance()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(toAccount.getBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("轉帳冪等性：相同 idempotencyKey 應直接回傳舊交易")
    void transfer_idempotent_returnsExistingTransaction() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey("key-duplicate");

        Transaction existingTxn = Transaction.builder()
                .id(1L)
                .txnNo("existing-txn")
                .type("TRANSFER")
                .status("SUCCESS")
                .build();

        when(transactionRepository.findByIdempotencyKey("key-duplicate")).thenReturn(Optional.of(existingTxn));

        // Act
        Transaction result = transferService.transfer(request);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTxnNo()).isEqualTo("existing-txn");
        verify(accountRepository, never()).findByIdForUpdate(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("轉帳失敗：餘額不足應拋出 BusinessException")
    void transfer_insufficientBalance_throwsException() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("9999.00"));
        request.setIdempotencyKey("key-002");

        Account fromAccount = Account.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .status("ACTIVE")
                .build();

        Account toAccount = Account.builder()
                .id(2L)
                .userId(2L)
                .balance(new BigDecimal("200.00"))
                .status("ACTIVE")
                .build();

        when(transactionRepository.findByIdempotencyKey("key-002")).thenReturn(Optional.empty());
        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("餘額不足")
                .extracting("code").isEqualTo("INSUFFICIENT_BALANCE");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("轉帳失敗：發送方帳戶被凍結應拋出 BusinessException")
    void transfer_frozenAccount_throwsException() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey("key-003");

        Account fromAccount = Account.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("500.00"))
                .status("FROZEN")
                .build();

        Account toAccount = Account.builder()
                .id(2L)
                .userId(2L)
                .balance(new BigDecimal("200.00"))
                .status("ACTIVE")
                .build();

        when(transactionRepository.findByIdempotencyKey("key-003")).thenReturn(Optional.empty());
        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("發送方帳戶已被凍結")
                .extracting("code").isEqualTo("ACCOUNT_FROZEN");
    }

    @Test
    @DisplayName("轉帳失敗：轉帳給自己應拋出 BusinessException")
    void transfer_selfTransfer_throwsException() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(1L);
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey("key-004");

        Account account = Account.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("500.00"))
                .status("ACTIVE")
                .build();

        when(transactionRepository.findByIdempotencyKey("key-004")).thenReturn(Optional.empty());
        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        // fromId == toId, so firstId == secondId, findByIdForUpdate called once returning same account
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能轉帳給自己")
                .extracting("code").isEqualTo("SELF_TRANSFER");
    }

    @Test
    @DisplayName("轉帳失敗：無權操作帳戶應拋出 BusinessException")
    void transfer_accessDenied_throwsException() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(5L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey("key-005");

        when(transactionRepository.findByIdempotencyKey("key-005")).thenReturn(Optional.empty());
        // 帳戶不屬於當前使用者 (userId=1 無法找到 accountId=5 的帳戶)
        when(accountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("ACCESS_DENIED");
    }

    // ========== topUp ==========

    @Test
    @DisplayName("儲值成功：應增加帳戶餘額並建立交易紀錄")
    void topUp_success() {
        // Arrange
        TopUpRequest request = new TopUpRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("500.00"));
        request.setIdempotencyKey("topup-001");
        request.setRemark("儲值");

        Account account = Account.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .status("ACTIVE")
                .build();

        Transaction savedTxn = Transaction.builder()
                .id(2L)
                .txnNo("123456789")
                .type("TOPUP")
                .status("SUCCESS")
                .build();

        when(transactionRepository.findByIdempotencyKey("topup-001")).thenReturn(Optional.empty());
        when(idGenerator.nextId()).thenReturn(123456789L);
        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTxn);

        // Act
        Transaction result = transferService.topUp(request);

        // Assert
        assertThat(result.getType()).isEqualTo("TOPUP");
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("600.00"));
        verify(accountRepository).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("儲值冪等性：相同 idempotencyKey 應直接回傳舊交易")
    void topUp_idempotent_returnsExistingTransaction() {
        // Arrange
        TopUpRequest request = new TopUpRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("500.00"));
        request.setIdempotencyKey("topup-dup");

        Transaction existingTxn = Transaction.builder()
                .id(2L)
                .txnNo("existing-topup")
                .type("TOPUP")
                .status("SUCCESS")
                .build();

        when(transactionRepository.findByIdempotencyKey("topup-dup")).thenReturn(Optional.of(existingTxn));

        // Act
        Transaction result = transferService.topUp(request);

        // Assert
        assertThat(result.getId()).isEqualTo(2L);
        verify(accountRepository, never()).findById(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("儲值失敗：帳戶不存在應拋出 BusinessException")
    void topUp_accountNotFound_throwsException() {
        // Arrange
        TopUpRequest request = new TopUpRequest();
        request.setAccountId(99L);
        request.setAmount(new BigDecimal("500.00"));
        request.setIdempotencyKey("topup-002");

        Account mockAccount = Account.builder().id(99L).userId(1L).build();
        when(transactionRepository.findByIdempotencyKey("topup-002")).thenReturn(Optional.empty());
        when(accountRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.of(mockAccount));
        when(accountRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transferService.topUp(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("ACCOUNT_NOT_FOUND");
    }

    // ========== withdraw ==========

    @Test
    @DisplayName("提現成功：餘額足夠且帳戶正常應扣款成功")
    void withdraw_success() {
        // Arrange
        WithdrawRequest request = new WithdrawRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("200.00"));
        request.setIdempotencyKey("withdraw-001");
        request.setRemark("提現");

        Account account = Account.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("500.00"))
                .status("ACTIVE")
                .build();

        Transaction savedTxn = Transaction.builder()
                .id(3L)
                .txnNo("123456789")
                .type("WITHDRAW")
                .status("SUCCESS")
                .build();

        when(transactionRepository.findByIdempotencyKey("withdraw-001")).thenReturn(Optional.empty());
        when(idGenerator.nextId()).thenReturn(123456789L);
        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTxn);

        // Act
        Transaction result = transferService.withdraw(request);

        // Assert
        assertThat(result.getType()).isEqualTo("WITHDRAW");
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
        verify(accountRepository).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("提現失敗：餘額不足應拋出 BusinessException")
    void withdraw_insufficientBalance_throwsException() {
        // Arrange
        WithdrawRequest request = new WithdrawRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("9999.00"));
        request.setIdempotencyKey("withdraw-002");

        Account account = Account.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .status("ACTIVE")
                .build();

        when(transactionRepository.findByIdempotencyKey("withdraw-002")).thenReturn(Optional.empty());
        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        // Act & Assert
        assertThatThrownBy(() -> transferService.withdraw(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("餘額不足")
                .extracting("code").isEqualTo("INSUFFICIENT_BALANCE");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("提現失敗：帳戶被凍結應拋出 BusinessException")
    void withdraw_frozenAccount_throwsException() {
        // Arrange
        WithdrawRequest request = new WithdrawRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey("withdraw-003");

        Account account = Account.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("500.00"))
                .status("FROZEN")
                .build();

        when(transactionRepository.findByIdempotencyKey("withdraw-003")).thenReturn(Optional.empty());
        when(accountRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        // Act & Assert
        assertThatThrownBy(() -> transferService.withdraw(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("帳戶已被凍結")
                .extracting("code").isEqualTo("ACCOUNT_FROZEN");
    }

    @Test
    @DisplayName("提現冪等性：相同 idempotencyKey 應直接回傳舊交易")
    void withdraw_idempotent_returnsExistingTransaction() {
        // Arrange
        WithdrawRequest request = new WithdrawRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey("withdraw-dup");

        Transaction existingTxn = Transaction.builder()
                .id(3L)
                .txnNo("existing-withdraw")
                .type("WITHDRAW")
                .status("SUCCESS")
                .build();

        when(transactionRepository.findByIdempotencyKey("withdraw-dup")).thenReturn(Optional.of(existingTxn));

        // Act
        Transaction result = transferService.withdraw(request);

        // Assert
        assertThat(result.getId()).isEqualTo(3L);
        verify(accountRepository, never()).findByIdForUpdate(any());
        verify(transactionRepository, never()).save(any());
    }
}

