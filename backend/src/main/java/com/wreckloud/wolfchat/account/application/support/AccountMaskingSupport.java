package com.wreckloud.wolfchat.account.application.support;

import org.springframework.util.StringUtils;

/**
 * 账号脱敏工具：用于日志与审计记录，避免输出完整账号。
 */
public final class AccountMaskingSupport {
    private AccountMaskingSupport() {
    }

    public static String maskAccount(String account) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        String normalizedAccount = account.trim();
        if (normalizedAccount.contains("@")) {
            return maskEmail(normalizedAccount);
        }
        return maskWolfNo(normalizedAccount);
    }

    private static String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***" + domainPart;
        }
        String start = localPart.substring(0, 1);
        String end = localPart.substring(localPart.length() - 1);
        return start + "***" + end + domainPart;
    }

    private static String maskWolfNo(String wolfNo) {
        if (wolfNo.length() <= 4) {
            return "***";
        }
        String start = wolfNo.substring(0, 2);
        String end = wolfNo.substring(wolfNo.length() - 2);
        return start + "****" + end;
    }
}

