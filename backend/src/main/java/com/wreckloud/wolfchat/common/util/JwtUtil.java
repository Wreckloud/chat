package com.wreckloud.wolfchat.common.util;

import com.wreckloud.wolfchat.common.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description JWT 工具类，用于生成和解析 JWT token
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {
    private final JwtConfig jwtConfig;

    /**
     * 生成 JWT token
     *
     * @param userId 行者ID
     * @return JWT token
     */
    public String generateToken(Long userId) {
        return generateToken(userId, 0L);
    }

    /**
     * 生成 JWT token
     *
     * @param userId 行者ID
     * @param passwordVersion 密码版本（秒级时间戳）
     * @return JWT token
     */
    public String generateToken(Long userId, Long passwordVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("pwdVer", passwordVersion == null ? 0L : passwordVersion);
        return createToken(claims);
    }

    /**
     * 创建 token
     */
    private String createToken(Map<String, Object> claims) {
        Date now = new Date();
        // 配置中的过期时间是秒数，需要转换为毫秒数
        Date expiration = new Date(now.getTime() + jwtConfig.getExpiration() * 1000);

        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从 token 中获取 Claims
     */
    public Claims getClaimsFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 token 中获取行者ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        return readLongClaim(claims, "userId");
    }

    /**
     * 从 token 中获取密码版本
     */
    public Long getPasswordVersionFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        return readLongClaim(claims, "pwdVer");
    }

    /**
     * 验证 token 是否有效
     */
    public boolean validateToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return false;
        }
        Date expiration = claims.getExpiration();
        return expiration.after(new Date());
    }

    private Long readLongClaim(Claims claims, String claimKey) {
        if (claims == null || claimKey == null) {
            return null;
        }
        Object claimValue = claims.get(claimKey);
        if (claimValue instanceof Integer) {
            return ((Integer) claimValue).longValue();
        }
        if (claimValue instanceof Long) {
            return (Long) claimValue;
        }
        return null;
    }
}


