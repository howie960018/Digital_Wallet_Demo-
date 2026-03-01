package com.wallet.digitalwallet.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.math.BigDecimal;

@Getter
public class TransactionEvent extends ApplicationEvent {

    private final String txnNo;
    private final String type;
    private final BigDecimal amount;
    private final Long fromAccountId;
    private final Long toAccountId;
    private final String status;
    private final String remark;

    public TransactionEvent(Object source, String txnNo, String type,
                            BigDecimal amount, Long fromAccountId,
                            Long toAccountId, String status, String remark) {
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