package com.wallet.digitalwallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AccountStatusRequest {

    @NotNull(message = "帳戶 ID 不可為空")
    private Long accountId;

    @NotBlank(message = "原因不可為空")
    private String reason;
}