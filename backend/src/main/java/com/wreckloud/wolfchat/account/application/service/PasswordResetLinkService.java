package com.wreckloud.wolfchat.account.application.service;

import com.wreckloud.wolfchat.account.config.EmailVerifyLinkConfig;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Locale;

/**
 * @Description 重置密码链接服务
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetLinkService {
    /**
     * 重置链接有效时长（分钟）
     */
    private static final int RESET_LINK_EXPIRE_MINUTES = 15;

    private static final String REDIS_KEY_PREFIX = "wolfchat:password-reset-link";

    private static final String PAYLOAD_SEPARATOR = "\n";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final DefaultRedisScript<String> GET_AND_DELETE_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); " +
                    "if value then redis.call('DEL', KEYS[1]); end; " +
                    "return value;",
            String.class
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final EmailSendLimitService emailSendLimitService;
    private final EmailVerifyLinkConfig emailVerifyLinkConfig;

    /**
     * 发送重置密码链接
     */
    public void sendResetLink(Long userId, String email) {
        if (userId == null || !StringUtils.hasText(email)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        String normalizedEmail = normalizeEmail(email);
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        emailSendLimitService.validateAndIncrease(REDIS_KEY_PREFIX, normalizedEmail);

        String token = generateToken();
        String tokenHash = hashToken(token);
        String payload = buildPayload(userId, normalizedEmail);
        ops.set(buildTokenKey(tokenHash), payload, Duration.ofMinutes(RESET_LINK_EXPIRE_MINUTES));

        String resetLink = buildResetLink(token);
        log.info(
                "重置密码链接(开发模式): userId={}, email={}, link={}, expireInMinutes={}",
                userId,
                normalizedEmail,
                resetLink,
                RESET_LINK_EXPIRE_MINUTES
        );
        log.info("重置密码链接发送成功: userId={}, email={}", userId, normalizedEmail);
    }

    /**
     * 校验并消费重置密码链接 token
     */
    public ResetTarget verifyAndConsumeToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        String tokenHash = hashToken(token.trim());
        String payload = stringRedisTemplate.execute(
                GET_AND_DELETE_SCRIPT,
                Collections.singletonList(buildTokenKey(tokenHash))
        );
        if (!StringUtils.hasText(payload)) {
            throw new BaseException(ErrorCode.PASSWORD_RESET_LINK_INVALID);
        }
        return parsePayload(payload);
    }

    /**
     * 判断重置密码链接 token 是否仍可用（未消费且未过期）
     */
    public boolean isTokenAvailable(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        String tokenHash = hashToken(token.trim());
        Boolean exists = stringRedisTemplate.hasKey(buildTokenKey(tokenHash));
        return Boolean.TRUE.equals(exists);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new BaseException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private String buildResetLink(String token) {
        return emailVerifyLinkConfig.getResetPasswordUrl() + "?token=" + token;
    }

    private String buildPayload(Long userId, String email) {
        return userId + PAYLOAD_SEPARATOR + email;
    }

    private ResetTarget parsePayload(String payload) {
        String[] parts = payload.split(PAYLOAD_SEPARATOR, -1);
        if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            throw new BaseException(ErrorCode.PASSWORD_RESET_LINK_INVALID);
        }

        Long userId;
        try {
            userId = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            throw new BaseException(ErrorCode.PASSWORD_RESET_LINK_INVALID);
        }
        return new ResetTarget(userId, parts[1]);
    }

    private String buildTokenKey(String tokenHash) {
        return REDIS_KEY_PREFIX + ":token:" + tokenHash;
    }

    @Getter
    @AllArgsConstructor
    public static class ResetTarget {
        private final Long userId;
        private final String email;
    }
}
