package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.TransferRequest;
import com.wallet.digitalwallet.entity.Transaction;
import com.wallet.digitalwallet.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
@Tag(name = "轉帳", description = "帳戶間轉帳")
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "轉帳", description = "從一個帳戶轉帳到另一個帳戶")
    @PostMapping
    public ResponseEntity<Transaction> transfer(@Valid @RequestBody TransferRequest request) {
        Transaction txn = transferService.transfer(request);
        return ResponseEntity.ok(txn);
    }
}