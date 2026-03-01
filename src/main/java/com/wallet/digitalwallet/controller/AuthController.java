package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.ApiResponse;
import com.wallet.digitalwallet.dto.LoginRequest;
import com.wallet.digitalwallet.dto.LoginResponse;
import com.wallet.digitalwallet.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "認證", description = "登入與身份驗證")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登入", description = "驗證帳號密碼並取得 JWT Token")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("登入成功", authService.login(request)));
    }
}