package com.wreckloud.wolfchat.account.application.service;

import com.wreckloud.wolfchat.account.domain.enums.EmailCodeScene;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @Description 邮箱验证码服务
 * @Author Wreckloud
 * @Date 2026-03-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeService {
    /**
     * 验证码有效时长（分钟）
     */
    private static final int VERIFY_CODE_EXPIRE_MINUTES = 10;

    /**
     * 最小发送间隔（秒）
     */
    private static final int SEND_INTERVAL_SECONDS = 60;

    /**
     * 单日发送上限（同邮箱 + 同场景）
     */
    private static final int DAILY_SEND_LIMIT = 20;

    /**
     * 过期时间标记保留时长（小时）
     */
    private static final int EXPIRE_MARKER_RETAIN_HOURS = 24;

    private static final String REDIS_KEY_PREFIX = "wolfchat:email-code";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 发送验证码
     */
    public void sendCode(String email, EmailCodeScene scene) {
        if (!StringUtils.hasText(email) || scene == null) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        String normalizedEmail = normalizeEmail(email);
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();

        String intervalKey = buildIntervalKey(normalizedEmail, scene);
        Boolean locked = ops.setIfAbsent(intervalKey, "1", Duration.ofSeconds(SEND_INTERVAL_SECONDS));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BaseException(ErrorCode.EMAIL_CODE_SEND_TOO_FREQUENT);
        }

        String dailyCountKey = buildDailyCountKey(normalizedEmail, scene, LocalDate.now());
        Long dailyCount = ops.increment(dailyCountKey);
        if (dailyCount == null) {
            stringRedisTemplate.delete(intervalKey);
            throw new BaseException(ErrorCode.SYSTEM_ERROR);
        }
        if (dailyCount == 1L) {
            long secondsToTomorrow = Duration.between(
                    LocalDateTime.now(),
                    LocalDate.now().plusDays(1).atStartOfDay()
            ).getSeconds();
            if (secondsToTomorrow <= 0) {
                secondsToTomorrow = 24 * 60 * 60;
            }
            stringRedisTemplate.expire(dailyCountKey, Duration.ofSeconds(secondsToTomorrow));
        }
        if (dailyCount > DAILY_SEND_LIMIT) {
            ops.decrement(dailyCountKey);
            stringRedisTemplate.delete(intervalKey);
            throw new BaseException(ErrorCode.EMAIL_CODE_DAILY_LIMIT);
        }

        String verifyCode = generateVerifyCode();
        long expireAtEpoch = LocalDateTime.now()
                .plusMinutes(VERIFY_CODE_EXPIRE_MINUTES)
                .toEpochSecond(ZoneOffset.UTC);

        ops.set(buildCodeKey(normalizedEmail, scene), verifyCode, Duration.ofMinutes(VERIFY_CODE_EXPIRE_MINUTES));
        ops.set(
                buildExpireMarkerKey(normalizedEmail, scene),
                String.valueOf(expireAtEpoch),
                Duration.ofHours(EXPIRE_MARKER_RETAIN_HOURS)
        );

        logVerifyCode(normalizedEmail, scene, verifyCode);
        log.info("邮箱验证码发送成功: email={}, scene={}", normalizedEmail, scene.getCode());
    }

    /**
     * 校验并消费验证码
     */
    public void verifyAndConsume(String email, EmailCodeScene scene, String verifyCode) {
        if (!StringUtils.hasText(email) || scene == null || !StringUtils.hasText(verifyCode)) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }

        String normalizedEmail = normalizeEmail(email);
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String codeKey = buildCodeKey(normalizedEmail, scene);

        String cachedCode = ops.get(codeKey);
        if (!StringUtils.hasText(cachedCode)) {
            if (isExpired(normalizedEmail, scene, ops)) {
                throw new BaseException(ErrorCode.EMAIL_CODE_EXPIRED);
            }
            throw new BaseException(ErrorCode.EMAIL_CODE_ERROR);
        }

        if (!verifyCode.equals(cachedCode)) {
            throw new BaseException(ErrorCode.EMAIL_CODE_ERROR);
        }

        Boolean deleted = stringRedisTemplate.delete(codeKey);
        if (!Boolean.TRUE.equals(deleted)) {
            throw new BaseException(ErrorCode.EMAIL_CODE_ERROR);
        }
    }

    private boolean isExpired(String email, EmailCodeScene scene, ValueOperations<String, String> ops) {
        String marker = ops.get(buildExpireMarkerKey(email, scene));
        if (!StringUtils.hasText(marker)) {
            return false;
        }

        long expireAtEpoch;
        try {
            expireAtEpoch = Long.parseLong(marker);
        } catch (NumberFormatException e) {
            return false;
        }
        long nowEpoch = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        return nowEpoch > expireAtEpoch;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateVerifyCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }

    private void logVerifyCode(String email, EmailCodeScene scene, String verifyCode) {
        log.info(
                "验证码(开发模式): email={}, scene={}, code={}, expireInMinutes={}",
                email,
                scene.getCode(),
                verifyCode,
                VERIFY_CODE_EXPIRE_MINUTES
        );
    }

    private String buildCodeKey(String email, EmailCodeScene scene) {
        return REDIS_KEY_PREFIX + ":code:" + scene.getCode() + ":" + email;
    }

    private String buildExpireMarkerKey(String email, EmailCodeScene scene) {
        return REDIS_KEY_PREFIX + ":expire:" + scene.getCode() + ":" + email;
    }

    private String buildIntervalKey(String email, EmailCodeScene scene) {
        return REDIS_KEY_PREFIX + ":interval:" + scene.getCode() + ":" + email;
    }

    private String buildDailyCountKey(String email, EmailCodeScene scene, LocalDate day) {
        return REDIS_KEY_PREFIX + ":daily:" + scene.getCode() + ":" + day + ":" + email;
    }
}
