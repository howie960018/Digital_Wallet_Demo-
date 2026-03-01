package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.ApiResponse;
import com.wallet.digitalwallet.entity.Notification;
import com.wallet.digitalwallet.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "通知", description = "交易通知查詢")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @Operation(summary = "查詢通知", description = "查詢指定帳戶的交易通知紀錄")
    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
            @PathVariable @NonNull Long accountId) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationRepository.findByAccountIdOrderByCreatedAtDesc(accountId)));
    }
}