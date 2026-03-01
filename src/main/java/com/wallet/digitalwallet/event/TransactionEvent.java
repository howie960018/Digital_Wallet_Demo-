package com.wallet.digitalwallet.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.springframework.lang.Nullable;
import java.math.BigDecimal;

@Getter
public class TransactionEvent extends ApplicationEvent {

    private final String txnNo;
    private final String type;
    private final BigDecimal amount;

    @Nullable
    private final Long fromAccountId;

    @Nullable
    private final Long toAccountId;

    private final String status;
    private final String remark;

    public TransactionEvent(Object source, String txnNo, String type,
                            BigDecimal amount, @Nullable Long fromAccountId,
                            @Nullable Long toAccountId, String status, String remark) {
        super(source);
        this.txnNo = txnNo;
        this.type = type;
        this.amount = amount;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.status = status;
        this.remark = remark;
    }
}