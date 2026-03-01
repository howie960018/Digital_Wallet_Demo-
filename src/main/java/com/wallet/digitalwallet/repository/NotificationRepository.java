package com.wallet.digitalwallet.repository;

import com.wallet.digitalwallet.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}