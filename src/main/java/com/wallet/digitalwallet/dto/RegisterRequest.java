package com.wallet.digitalwallet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {

    @NotBlank(message = "使用者名稱不可為空")
    @Size(min = 3, max = 50, message = "使用者名稱長度需在 3-50 之間")
    private String username;

    @NotBlank(message = "Email 不可為空")
    @Email(message = "Email 格式不正確")
    private String email;

    @NotBlank(message = "密碼不可為空")
    @Size(min = 6, message = "密碼長度至少 6 位")
    private String password;
}