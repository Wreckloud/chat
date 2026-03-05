package com.wreckloud.wolfchat.account.application.service;

import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @Description 邮件发送频控服务
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Service
@RequiredArgsConstructor
public class EmailSendLimitService {
    /**
     * 最小发送间隔（秒）
     */
    private static final int SEND_INTERVAL_SECONDS = 60;

    /**
     * 单日发送上限（同邮箱）
     */
    private static final int DAILY_SEND_LIMIT = 20;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 校验并增加发送计数
     *
     * @param keyPrefix 键前缀，不同业务场景用不同前缀隔离
     * @param email     已标准化邮箱
     */
    public void validateAndIncrease(String keyPrefix, String email) {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();

        String intervalKey = buildIntervalKey(keyPrefix, email);
        Boolean locked = ops.setIfAbsent(intervalKey, "1", Duration.ofSeconds(SEND_INTERVAL_SECONDS));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BaseException(ErrorCode.EMAIL_SEND_TOO_FREQUENT);
        }

        String dailyCountKey = buildDailyCountKey(keyPrefix, email, LocalDate.now());
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
            throw new BaseException(ErrorCode.EMAIL_DAILY_LIMIT);
        }
    }

    private String buildIntervalKey(String keyPrefix, String email) {
        return keyPrefix + ":interval:" + email;
    }

    private String buildDailyCountKey(String keyPrefix, String email, LocalDate day) {
        return keyPrefix + ":daily:" + day + ":" + email;
    }
}
