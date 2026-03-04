package com.wreckloud.wolfchat.chat.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
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
    private List<String> allowedOriginPatterns = new ArrayList<>(Arrays.asList(
            "http://localhost:*",
            "https://localhost:*",
            "https://servicewechat.com",
            "https://*.servicewechat.com"
    ));
}
