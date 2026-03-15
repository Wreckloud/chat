package com.wreckloud.wolfchat.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

/**
 * @Description JWT 配置类，从 application.yml 中读取 jwt.secret 和 jwt.expiration（单位：秒）
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private static final int JWT_SECRET_MIN_BYTES = 32;

    /**
     * JWT 密钥
     */
    private String secret;

    /**
     * JWT 过期时间（秒），默认7天（604800秒）
     */
    private Long expiration;

    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("配置缺失: jwt.secret");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < JWT_SECRET_MIN_BYTES) {
            throw new IllegalArgumentException("配置非法: jwt.secret 至少需要 32 字节");
        }
        if (expiration == null || expiration < 1) {
            throw new IllegalArgumentException("配置非法: jwt.expiration 必须 >= 1");
        }
    }
}


