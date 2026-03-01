package com.wallet.digitalwallet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "txn_no", nullable = false)
    private String txnNo;

    @Column(nullable = false, length = 20)
    private String channel;  // EMAIL, SMS, IN_APP

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false, length = 20)
    private String status;  // SENT, FAILED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}