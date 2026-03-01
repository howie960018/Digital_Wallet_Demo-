package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.ApiResponse;
import com.wallet.digitalwallet.dto.WithdrawRequest;
import com.wallet.digitalwallet.entity.Transaction;
import com.wallet.digitalwallet.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/withdraw")
@RequiredArgsConstructor
@Tag(name = "提現", description = "帳戶提現")
public class WithdrawController {

    private final TransferService transferService;

    @Operation(summary = "提現")
    @PostMapping
    public ResponseEntity<ApiResponse<Transaction>> withdraw(@Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(ApiResponse.success("提現成功", transferService.withdraw(request)));
    }
}