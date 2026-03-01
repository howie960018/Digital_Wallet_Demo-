package com.wallet.digitalwallet.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TransactionResponse {

    private String txnNo;
    private String type;
    private BigDecimal amount;
    private String status;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal balanceAfterTxn;
    private String remark;
    private LocalDateTime createdAt;
}