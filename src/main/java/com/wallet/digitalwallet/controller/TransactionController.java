package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.TransactionResponse;
import com.wallet.digitalwallet.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransferService transferService;

    @GetMapping("/{accountId}")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @PathVariable Long accountId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(transferService.getTransactions(accountId, pageable));
    }
}