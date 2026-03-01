package com.wallet.digitalwallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AccountStatusRequest {

    @NotNull(message = "Account ID must not be null")
    private Long accountId;

    @NotBlank(message = "Reason must not be blank")
    private String reason;
}