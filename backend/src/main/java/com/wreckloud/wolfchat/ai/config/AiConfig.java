package com.wreckloud.wolfchat.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Description AI 能力配置（预留）
 * @Author Wreckloud
 * @Date 2026-03-20
 */
@Data
@Component
@ConfigurationProperties(prefix = "wolfchat.ai")
public class AiConfig {
    private Boolean enabled;
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String model;
    private Double temperature;
    private Integer maxOutputTokens;
    private Integer timeoutMs;

    private Memory memory = new Memory();
    private Lobby lobby = new Lobby();

    @Data
    public static class Memory {
        private Boolean enabled;
        private Integer recentWindowSize;
        private Integer summaryTriggerMessageCount;
        private Integer summaryMaxTokens;
        private String embeddingModel;
        private Integer topK;
    }

    @Data
    public static class Lobby {
        private Boolean enabled;
        private Long botUserId;
        private String botDisplayName;
        private Integer cooldownSeconds;
        private Double replyProbability;
        private Integer maxRepliesPerHour;
    }
}

