package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.WithdrawRequest;
import com.wallet.digitalwallet.entity.Transaction;
import com.wallet.digitalwallet.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/withdraw")
@RequiredArgsConstructor
public class WithdrawController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<Transaction> withdraw(@Valid @RequestBody WithdrawRequest request) {
        Transaction txn = transferService.withdraw(request);
        return ResponseEntity.ok(txn);
    }
}