package com.wallet.digitalwallet.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class RegisterResponse {

    private Long userId;
    private String username;
    private String email;
    private Long accountId;
    private BigDecimal balance;
    private String currency;
}