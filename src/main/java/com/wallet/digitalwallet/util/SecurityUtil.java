package com.wallet.digitalwallet.util;

import com.wallet.digitalwallet.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    private SecurityUtil() {}

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BusinessException("UNAUTHORIZED", "未登入");
        }
        return (Long) auth.getPrincipal();
    }
}