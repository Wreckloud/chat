package com.wreckloud.wolfchat.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Description JWT 配置类，从 application.yml 中读取 jwt.secret 和 jwt.expiration（单位：秒）
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    /**
     * JWT 密钥
     */
    private String secret;

    /**
     * JWT 过期时间（秒），默认7天（604800秒）
     */
    private Long expiration = 604800L;
}


