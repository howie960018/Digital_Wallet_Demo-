package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.WithdrawRequest;
import com.wallet.digitalwallet.entity.Transaction;
import com.wallet.digitalwallet.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/withdraw")
@RequiredArgsConstructor
@Tag(name = "提現", description = "帳戶提現")
public class WithdrawController {

    private final TransferService transferService;

    @Operation(summary = "提現", description = "從指定帳戶提領至外部")
    @PostMapping
    public ResponseEntity<Transaction> withdraw(@Valid @RequestBody WithdrawRequest request) {
        Transaction txn = transferService.withdraw(request);
        return ResponseEntity.ok(txn);
    }
}