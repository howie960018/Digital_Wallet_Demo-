package com.wallet.digitalwallet.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class AccountResponse {

    private Long accountId;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private String status;
}