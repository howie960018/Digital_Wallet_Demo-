package com.wallet.digitalwallet.event;

import com.wallet.digitalwallet.entity.Notification;
import com.wallet.digitalwallet.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationRepository notificationRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransactionEvent(TransactionEvent event) {

        log.info("[通知系統] 收到交易事件：type={}, txnNo={}, amount={}",
                event.getType(), event.getTxnNo(), event.getAmount());

        // 通知發送方（提現、轉帳）
        if (event.getFromAccountId() != null) {
            String content = buildMessage(event, "out");
            saveNotification(event.getFromAccountId(), event.getTxnNo(), content);
        }

        // 通知接收方（儲值、轉帳）
        if (event.getToAccountId() != null) {
            String content = buildMessage(event, "in");
            saveNotification(event.getToAccountId(), event.getTxnNo(), content);
        }
    }

    private String buildMessage(TransactionEvent event, String direction) {
        return switch (event.getType()) {
            case "TRANSFER" -> direction.equals("out")
                    ? String.format("您已成功轉出 %s 元，交易編號：%s", event.getAmount(), event.getTxnNo())
                    : String.format("您已收到轉入 %s 元，交易編號：%s", event.getAmount(), event.getTxnNo());
            case "TOPUP" -> String.format("儲值成功 %s 元，交易編號：%s", event.getAmount(), event.getTxnNo());
            case "WITHDRAW" -> String.format("提現成功 %s 元，交易編號：%s", event.getAmount(), event.getTxnNo());
            default -> String.format("交易完成，金額 %s 元，交易編號：%s", event.getAmount(), event.getTxnNo());
        };
    }

    private void saveNotification(Long accountId, String txnNo, String content) {
        try {
            Notification notification = Notification.builder()
                    .accountId(accountId)
                    .txnNo(txnNo)
                    .channel("IN_APP")
                    .content(content)
                    .status("SENT")
                    .build();

            notificationRepository.save(notification);
            log.info("[通知系統] 通知已儲存：accountId={}, content={}", accountId, content);

        } catch (Exception e) {
            log.error("[通知系統] 通知儲存失敗：accountId={}, error={}", accountId, e.getMessage());
            // 通知失敗不影響交易，只記錄錯誤
        }
    }
}