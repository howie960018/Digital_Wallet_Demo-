package com.wallet.digitalwallet.controller;

import com.wallet.digitalwallet.dto.ApiResponse;
import com.wallet.digitalwallet.dto.TopUpRequest;
import com.wallet.digitalwallet.entity.Transaction;
import com.wallet.digitalwallet.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/topup")
@RequiredArgsConstructor
@Tag(name = "儲值", description = "帳戶儲值")
public class TopUpController {

    private final TransferService transferService;

    @Operation(summary = "儲值")
    @PostMapping
    public ResponseEntity<ApiResponse<Transaction>> topUp(@Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.ok(ApiResponse.success("儲值成功", transferService.topUp(request)));
    }
}