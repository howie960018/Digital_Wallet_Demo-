package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.*;
import com.wallet.digitalwallet.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "使用者", description = "註冊與帳戶查詢")
public class UserController {

    private final UserService userService;

    @Operation(summary = "使用者註冊")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("註冊成功", userService.register(request)));
    }

    @Operation(summary = "查詢帳戶餘額")
    @GetMapping("/{userId}/accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccounts(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAccounts(userId)));
    }

    @Operation(summary = "凍結帳戶")
    @PostMapping("/accounts/freeze")
    public ResponseEntity<ApiResponse<AccountResponse>> freeze(@Valid @RequestBody AccountStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("帳戶已凍結", userService.freezeAccount(request)));
    }

    @Operation(summary = "解凍帳戶")
    @PostMapping("/accounts/unfreeze")
    public ResponseEntity<ApiResponse<AccountResponse>> unfreeze(@Valid @RequestBody AccountStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("帳戶已解凍", userService.unfreezeAccount(request)));
    }
}