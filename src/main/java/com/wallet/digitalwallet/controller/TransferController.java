package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.TransferRequest;
import com.wallet.digitalwallet.entity.Transaction;
import com.wallet.digitalwallet.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<Transaction> transfer(@Valid @RequestBody TransferRequest request) {
        Transaction txn = transferService.transfer(request);
        return ResponseEntity.ok(txn);
    }
}