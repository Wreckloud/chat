package com.wreckloud.wolfchat.account.application.service;

import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

/**
 * @Description 登录风控服务（失败计数与短期锁定）
 * @Author Wreckloud
 * @Date 2026-03-20
 */
@Service
@RequiredArgsConstructor
public class LoginRiskControlService {
    private static final String REDIS_KEY_PREFIX = "wolfchat:login-risk";
    private static final Duration FAIL_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final int ACCOUNT_FAIL_THRESHOLD = 6;
    private static final int IP_FAIL_THRESHOLD = 30;

    private final StringRedisTemplate stringRedisTemplate;

    public void assertCanLogin(String account, String clientIp) {
        String accountHash = hash(account);
        String ipHash = hash(clientIp);
        if (isLocked(buildAccountLockKey(accountHash)) || isLocked(buildIpLockKey(ipHash))) {
            throw new BaseException(ErrorCode.LOGIN_TOO_MANY_ATTEMPTS);
        }
    }

    public void recordFailure(String account, String clientIp) {
        String accountHash = hash(account);
        String ipHash = hash(clientIp);

        long accountFailCount = increaseFailCount(buildAccountFailKey(accountHash));
        if (accountFailCount >= ACCOUNT_FAIL_THRESHOLD) {
            lock(buildAccountLockKey(accountHash));
        }

        long ipFailCount = increaseFailCount(buildIpFailKey(ipHash));
        if (ipFailCount >= IP_FAIL_THRESHOLD) {
            lock(buildIpLockKey(ipHash));
        }
    }

    public void clearFailure(String account, String clientIp) {
        String accountHash = hash(account);
        String ipHash = hash(clientIp);
        stringRedisTemplate.delete(buildAccountFailKey(accountHash));
        stringRedisTemplate.delete(buildAccountLockKey(accountHash));
        stringRedisTemplate.delete(buildIpFailKey(ipHash));
    }

    public long getAccountFailCount(String account) {
        return readLongValue(buildAccountFailKey(hash(account)));
    }

    public long getIpFailCount(String clientIp) {
        return readLongValue(buildIpFailKey(hash(clientIp)));
    }

    public boolean isAccountLocked(String account) {
        return isLocked(buildAccountLockKey(hash(account)));
    }

    public boolean isIpLocked(String clientIp) {
        return isLocked(buildIpLockKey(hash(clientIp)));
    }

    public long getAccountLockTtlSeconds(String account) {
        return readTtlSeconds(buildAccountLockKey(hash(account)));
    }

    public long getIpLockTtlSeconds(String clientIp) {
        return readTtlSeconds(buildIpLockKey(hash(clientIp)));
    }

    public long countAccountLocks() {
        return countKeysByPattern(REDIS_KEY_PREFIX + ":account:lock:*");
    }

    public long countIpLocks() {
        return countKeysByPattern(REDIS_KEY_PREFIX + ":ip:lock:*");
    }

    public long countAccountFailBuckets() {
        return countKeysByPattern(REDIS_KEY_PREFIX + ":account:fail:*");
    }

    public long countIpFailBuckets() {
        return countKeysByPattern(REDIS_KEY_PREFIX + ":ip:fail:*");
    }

    private long increaseFailCount(String key) {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        Long failCount = ops.increment(key);
        if (failCount == null) {
            throw new BaseException(ErrorCode.SYSTEM_ERROR);
        }
        if (failCount == 1L) {
            stringRedisTemplate.expire(key, FAIL_WINDOW);
        }
        return failCount;
    }

    private void lock(String lockKey) {
        stringRedisTemplate.opsForValue().set(lockKey, "1", LOCK_DURATION);
    }

    private boolean isLocked(String lockKey) {
        Boolean exists = stringRedisTemplate.hasKey(lockKey);
        return Boolean.TRUE.equals(exists);
    }

    private long readLongValue(String key) {
        String rawValue = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(rawValue)) {
            return 0L;
        }
        try {
            long value = Long.parseLong(rawValue);
            return Math.max(value, 0L);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private long readTtlSeconds(String key) {
        Long ttl = stringRedisTemplate.getExpire(key);
        if (ttl == null || ttl < 0L) {
            return 0L;
        }
        return ttl;
    }

    private long countKeysByPattern(String pattern) {
        Long count = stringRedisTemplate.execute((RedisCallback<Long>) connection -> doCountKeys(connection, pattern));
        if (count == null) {
            return 0L;
        }
        return count;
    }

    private long doCountKeys(RedisConnection connection, String pattern) {
        if (connection == null) {
            return 0L;
        }
        long total = 0L;
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(500).build();
        try (Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                cursor.next();
                total++;
            }
        } catch (Exception e) {
            return 0L;
        }
        return total;
    }

    private String hash(String value) {
        if (!StringUtils.hasText(value)) {
            return "EMPTY";
        }
        try {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new BaseException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private String buildAccountFailKey(String accountHash) {
        return REDIS_KEY_PREFIX + ":account:fail:" + accountHash;
    }

    private String buildAccountLockKey(String accountHash) {
        return REDIS_KEY_PREFIX + ":account:lock:" + accountHash;
    }

    private String buildIpFailKey(String ipHash) {
        return REDIS_KEY_PREFIX + ":ip:fail:" + ipHash;
    }

    private String buildIpLockKey(String ipHash) {
        return REDIS_KEY_PREFIX + ":ip:lock:" + ipHash;
    }
}
