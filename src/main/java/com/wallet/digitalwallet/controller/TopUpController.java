package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.TopUpRequest;
import com.wallet.digitalwallet.entity.Transaction;
import com.wallet.digitalwallet.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/topup")
@RequiredArgsConstructor
public class TopUpController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<Transaction> topUp(@Valid @RequestBody TopUpRequest request) {
        Transaction txn = transferService.topUp(request);
        return ResponseEntity.ok(txn);
    }
}