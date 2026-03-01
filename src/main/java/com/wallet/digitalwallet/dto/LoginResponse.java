package com.wallet.digitalwallet.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private Long userId;
    private String username;
    private String token;
}