package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.ApiResponse;
import com.wallet.digitalwallet.dto.TransactionResponse;
import com.wallet.digitalwallet.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "交易紀錄", description = "查詢交易歷史")
public class TransactionController {

    private final TransferService transferService;

    @Operation(summary = "查詢交易紀錄")
    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @PathVariable Long accountId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(transferService.getTransactions(accountId, pageable)));
    }
}