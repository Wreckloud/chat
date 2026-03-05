package com.wreckloud.wolfchat.chat.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @Description WebSocket 配置项
 * @Author Wreckloud
 * @Date 2026-03-04
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wolfchat.ws")
public class WsConfig {
    /**
     * 允许的跨域来源模式
     */
    private List<String> allowedOriginPatterns;

    @PostConstruct
    public void validate() {
        if (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()) {
            throw new IllegalArgumentException("配置缺失: wolfchat.ws.allowed-origin-patterns");
        }
        for (String pattern : allowedOriginPatterns) {
            if (!StringUtils.hasText(pattern)) {
                throw new IllegalArgumentException("配置非法: wolfchat.ws.allowed-origin-patterns 不能为空字符串");
            }
        }
    }
}
