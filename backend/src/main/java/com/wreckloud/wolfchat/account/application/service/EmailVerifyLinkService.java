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
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * @Description 邮箱认证链接服务
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerifyLinkService {
    /**
     * 认证链接有效时长（分钟）
     */
    private static final int VERIFY_LINK_EXPIRE_MINUTES = 15;

    /**
     * 已消费 token 保留时长（小时）
     * 用于同一链接重复点击时返回幂等成功
     */
    private static final int USED_TOKEN_RETAIN_HOURS = 24;

    private static final String REDIS_KEY_PREFIX = "wolfchat:email-verify-link";

    private static final String PAYLOAD_SEPARATOR = "\n";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final DefaultRedisScript<String> CONSUME_AND_MARK_USED_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); " +
                    "if value then " +
                    "redis.call('DEL', KEYS[1]); " +
                    "redis.call('SETEX', KEYS[2], ARGV[1], value); " +
                    "end; " +
                    "return value;",
            String.class
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final EmailSendLimitService emailSendLimitService;
    private final EmailVerifyLinkConfig emailVerifyLinkConfig;

    /**
     * 发送绑定邮箱认证链接
     */
    public void sendBindVerifyLink(Long userId, String email) {
        if (userId == null || !StringUtils.hasText(email)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        String normalizedEmail = normalizeEmail(email);
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        emailSendLimitService.validateAndIncrease(REDIS_KEY_PREFIX, normalizedEmail);

        String token = generateToken();
        String tokenHash = hashToken(token);
        String tokenKey = buildTokenKey(tokenHash);
        String payload = buildPayload(userId, normalizedEmail);
        ops.set(tokenKey, payload, Duration.ofMinutes(VERIFY_LINK_EXPIRE_MINUTES));

        String verifyLink = buildVerifyLink(token);
        log.debug("邮箱认证链接已生成: userId={}, email={}, link={}", userId, normalizedEmail, verifyLink);
        log.info("邮箱认证链接发送成功: userId={}", userId);
    }

    /**
     * 校验并消费认证链接 token
     */
    public VerifyTarget verifyAndConsumeToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        String tokenHash = hashToken(token.trim());
        String usedTokenKey = buildUsedTokenKey(tokenHash);
        String usedTokenRetainSeconds = String.valueOf(Duration.ofHours(USED_TOKEN_RETAIN_HOURS).getSeconds());
        String payload = stringRedisTemplate.execute(
                CONSUME_AND_MARK_USED_SCRIPT,
                Arrays.asList(buildTokenKey(tokenHash), usedTokenKey),
                usedTokenRetainSeconds
        );
        if (StringUtils.hasText(payload)) {
            return parsePayload(payload);
        }

        String usedPayload = stringRedisTemplate.opsForValue().get(usedTokenKey);
        if (StringUtils.hasText(usedPayload)) {
            return parsePayload(usedPayload);
        }

        throw new BaseException(ErrorCode.EMAIL_VERIFY_LINK_INVALID);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
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

    private String buildVerifyLink(String token) {
        return emailVerifyLinkConfig.getVerifyUrl() + "?token=" + token;
    }

    private String buildPayload(Long userId, String email) {
        return userId + PAYLOAD_SEPARATOR + email;
    }

    private VerifyTarget parsePayload(String payload) {
        String[] parts = payload.split(PAYLOAD_SEPARATOR, -1);
        if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            throw new BaseException(ErrorCode.EMAIL_VERIFY_LINK_INVALID);
        }

        Long userId;
        try {
            userId = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            throw new BaseException(ErrorCode.EMAIL_VERIFY_LINK_INVALID);
        }
        return new VerifyTarget(userId, parts[1]);
    }

    private String buildTokenKey(String tokenHash) {
        return REDIS_KEY_PREFIX + ":token:" + tokenHash;
    }

    private String buildUsedTokenKey(String tokenHash) {
        return REDIS_KEY_PREFIX + ":token:used:" + tokenHash;
    }
    @Getter
    @AllArgsConstructor
    public static class VerifyTarget {
        private final Long userId;
        private final String email;
    }
}
