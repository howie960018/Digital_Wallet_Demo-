package com.wallet.digitalwallet.service;

import com.wallet.digitalwallet.entity.Notification;
import com.wallet.digitalwallet.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Get notifications for a specific account, ordered by creation time (newest first)
     * @param accountId the account ID
     * @return list of notifications
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByAccountId(Long accountId) {
        return notificationRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }
}

