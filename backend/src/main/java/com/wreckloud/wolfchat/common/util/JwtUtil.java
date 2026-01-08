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
     * @param wolfNo 狼藉号
     * @return JWT token
     */
    public String generateToken(Long userId, String wolfNo) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("wolfNo", wolfNo);
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
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    /**
     * 从 token 中获取狼藉号
     */
    public String getWolfNoFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        return (String) claims.get("wolfNo");
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
}


