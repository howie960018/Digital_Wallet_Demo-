package com.wallet.digitalwallet.repository;

import com.wallet.digitalwallet.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}

