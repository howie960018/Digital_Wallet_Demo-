package com.wallet.digitalwallet.repository;

import com.wallet.digitalwallet.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @NonNull
    List<Notification> findByAccountIdOrderByCreatedAtDesc(@NonNull Long accountId);
}