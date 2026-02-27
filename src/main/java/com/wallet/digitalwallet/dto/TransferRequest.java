package com.wallet.digitalwallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class TransferRequest {

    @NotNull(message = "發送方帳戶不可為空")
    private Long fromAccountId;

    @NotNull(message = "接收方帳戶不可為空")
    private Long toAccountId;

    @NotNull(message = "金額不可為空")
    @DecimalMin(value = "0.0001", message = "金額必須大於零")
    private BigDecimal amount;

    @NotBlank(message = "冪等鍵不可為空")
    private String idempotencyKey;

    private String remark;
}