package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.AccountResponse;
import com.wallet.digitalwallet.dto.RegisterRequest;
import com.wallet.digitalwallet.dto.RegisterResponse;
import com.wallet.digitalwallet.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.wallet.digitalwallet.dto.AccountStatusRequest;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "使用者", description = "註冊與帳戶查詢")
public class UserController {

    private final UserService userService;

    @Operation(summary = "使用者註冊", description = "註冊新使用者並自動建立 TWD 錢包")
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @Operation(summary = "查詢帳戶餘額", description = "查詢指定使用者的所有錢包餘額")
    @GetMapping("/{userId}/accounts")
    public ResponseEntity<List<AccountResponse>> getAccounts(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getAccounts(userId));
    }

    @Operation(summary = "凍結帳戶", description = "凍結指定帳戶，無法轉出和提現")
    @PostMapping("/accounts/freeze")
    public ResponseEntity<AccountResponse> freeze(@Valid @RequestBody AccountStatusRequest request) {
        return ResponseEntity.ok(userService.freezeAccount(request));
    }

    @Operation(summary = "解凍帳戶", description = "解凍指定帳戶，恢復正常功能")
    @PostMapping("/accounts/unfreeze")
    public ResponseEntity<AccountResponse> unfreeze(@Valid @RequestBody AccountStatusRequest request) {
        return ResponseEntity.ok(userService.unfreezeAccount(request));
    }
}