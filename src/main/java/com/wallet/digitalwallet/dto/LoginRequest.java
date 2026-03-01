package com.wallet.digitalwallet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginRequest {

    @NotBlank(message = "使用者名稱不可為空")
    private String username;

    @NotBlank(message = "密碼不可為空")
    private String password;
}