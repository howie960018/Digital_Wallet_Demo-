package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.AccountResponse;
import com.wallet.digitalwallet.dto.RegisterRequest;
import com.wallet.digitalwallet.dto.RegisterResponse;
import com.wallet.digitalwallet.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @GetMapping("/{userId}/accounts")
    public ResponseEntity<List<AccountResponse>> getAccounts(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getAccounts(userId));
    }
}