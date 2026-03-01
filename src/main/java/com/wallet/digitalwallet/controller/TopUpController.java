package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.TopUpRequest;
import com.wallet.digitalwallet.entity.Transaction;
import com.wallet.digitalwallet.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/topup")
@RequiredArgsConstructor
@Tag(name = "儲值", description = "帳戶儲值")
public class TopUpController {

    private final TransferService transferService;

    @Operation(summary = "儲值", description = "從外部來源儲值到指定帳戶")
    @PostMapping
    public ResponseEntity<Transaction> topUp(@Valid @RequestBody TopUpRequest request) {
        Transaction txn = transferService.topUp(request);
        return ResponseEntity.ok(txn);
    }
}