package com.wreckloud.wolfchat.account.application.service;

import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 改密邮箱验证码服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordChangeCodeService {
    private static final String REDIS_KEY_PREFIX = "wolfchat:password-change-code";
    private static final String PAYLOAD_SEPARATOR = "\n";
    private static final int CODE_EXPIRE_MINUTES = 10;
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final int CODE_DIGITS = 6;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;
    private final EmailSendLimitService emailSendLimitService;
    private final EmailDeliveryService emailDeliveryService;

    public void sendChangeCode(Long userId, String email) {
        if (userId == null || !StringUtils.hasText(email)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        String normalizedEmail = normalizeEmail(email);
        emailSendLimitService.validateAndIncrease(REDIS_KEY_PREFIX, normalizedEmail);

        String code = generateCode();
        String payload = buildPayload(normalizedEmail, hashText(code));
        String codeKey = buildCodeKey(userId);
        String triesKey = buildTriesKey(userId);

        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set(codeKey, payload, Duration.ofMinutes(CODE_EXPIRE_MINUTES));
        stringRedisTemplate.delete(triesKey);
        try {
            emailDeliveryService.sendPasswordChangeCode(normalizedEmail, code, CODE_EXPIRE_MINUTES);
        } catch (Exception e) {
            stringRedisTemplate.delete(codeKey);
            stringRedisTemplate.delete(triesKey);
            throw e;
        }
        log.info("改密验证码发送成功: userId={}, email={}", userId, normalizedEmail);
    }

    public void verifyAndConsumeCode(Long userId, String email, String code) {
        if (userId == null || !StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        String normalizedEmail = normalizeEmail(email);
        String normalizedCode = normalizeCode(code);
        String codeKey = buildCodeKey(userId);
        String triesKey = buildTriesKey(userId);
        String payload = stringRedisTemplate.opsForValue().get(codeKey);
        if (!StringUtils.hasText(payload)) {
            throw new BaseException(ErrorCode.PASSWORD_CHANGE_CODE_INVALID);
        }
        ParsedPayload parsedPayload = parsePayload(payload);
        if (!normalizedEmail.equals(parsedPayload.email)) {
            throw new BaseException(ErrorCode.PASSWORD_CHANGE_CODE_INVALID);
        }
        Integer currentTries = parseInt(stringRedisTemplate.opsForValue().get(triesKey), 0);
        if (currentTries >= MAX_VERIFY_ATTEMPTS) {
            throw new BaseException(ErrorCode.PASSWORD_CHANGE_CODE_LOCKED);
        }

        String expectedHash = parsedPayload.codeHash;
        if (!expectedHash.equals(hashText(normalizedCode))) {
            long seconds = resolveKeyTtlSeconds(codeKey);
            long tries = stringRedisTemplate.opsForValue().increment(triesKey);
            if (seconds > 0) {
                stringRedisTemplate.expire(triesKey, Duration.ofSeconds(seconds));
            }
            if (tries >= MAX_VERIFY_ATTEMPTS) {
                throw new BaseException(ErrorCode.PASSWORD_CHANGE_CODE_LOCKED);
            }
            throw new BaseException(ErrorCode.PASSWORD_CHANGE_CODE_INVALID);
        }

        stringRedisTemplate.delete(codeKey);
        stringRedisTemplate.delete(triesKey);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCode(String code) {
        String normalized = code.trim();
        if (normalized.length() != CODE_DIGITS) {
            throw new BaseException(ErrorCode.PASSWORD_CHANGE_CODE_INVALID);
        }
        for (int i = 0; i < normalized.length(); i++) {
            if (!Character.isDigit(normalized.charAt(i))) {
                throw new BaseException(ErrorCode.PASSWORD_CHANGE_CODE_INVALID);
            }
        }
        return normalized;
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, CODE_DIGITS);
        int min = bound / 10;
        int value = min + SECURE_RANDOM.nextInt(bound - min);
        return String.valueOf(value);
    }

    private String buildPayload(String email, String codeHash) {
        return email + PAYLOAD_SEPARATOR + codeHash;
    }

    private ParsedPayload parsePayload(String payload) {
        String[] parts = payload.split(PAYLOAD_SEPARATOR, -1);
        if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            throw new BaseException(ErrorCode.PASSWORD_CHANGE_CODE_INVALID);
        }
        return new ParsedPayload(parts[0], parts[1]);
    }

    private String buildCodeKey(Long userId) {
        return REDIS_KEY_PREFIX + ":code:" + userId;
    }

    private String buildTriesKey(Long userId) {
        return REDIS_KEY_PREFIX + ":tries:" + userId;
    }

    private String hashText(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new BaseException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private long resolveKeyTtlSeconds(String key) {
        Long expireSeconds = stringRedisTemplate.getExpire(key);
        if (expireSeconds == null || expireSeconds <= 0L) {
            return Duration.ofMinutes(CODE_EXPIRE_MINUTES).getSeconds();
        }
        return expireSeconds;
    }

    private Integer parseInt(String text, int fallback) {
        if (!StringUtils.hasText(text)) {
            return fallback;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static final class ParsedPayload {
        private final String email;
        private final String codeHash;

        private ParsedPayload(String email, String codeHash) {
            this.email = email;
            this.codeHash = codeHash;
        }
    }
}

