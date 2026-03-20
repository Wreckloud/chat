package com.wreckloud.wolfchat.common.util;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * @Description 客户端 IP 提取工具（登录风控、登录审计统一使用）
 * @Author Wreckloud
 * @Date 2026-03-20
 */
public final class ClientIpSupport {
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String UNKNOWN = "unknown";

    private ClientIpSupport() {
    }

    /**
     * 解析客户端 IP。
     * 仅当请求来自本机代理时，才信任 X-Forwarded-For，避免客户端伪造来源地址。
     */
    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String remoteAddr = trimToNull(request.getRemoteAddr());
        if (isLocalProxy(remoteAddr)) {
            String forwardedFor = trimToNull(request.getHeader(FORWARDED_FOR_HEADER));
            String forwardedClientIp = parseFirstForwardedIp(forwardedFor);
            if (StringUtils.hasText(forwardedClientIp)) {
                return forwardedClientIp;
            }
        }
        return remoteAddr;
    }

    private static String parseFirstForwardedIp(String forwardedFor) {
        if (!StringUtils.hasText(forwardedFor)) {
            return null;
        }
        return Arrays.stream(forwardedFor.split(","))
                .map(ClientIpSupport::trimToNull)
                .filter(StringUtils::hasText)
                .filter(ip -> !UNKNOWN.equalsIgnoreCase(ip))
                .findFirst()
                .orElse(null);
    }

    private static boolean isLocalProxy(String ip) {
        return "127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip);
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
