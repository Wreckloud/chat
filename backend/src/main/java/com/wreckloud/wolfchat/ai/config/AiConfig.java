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
    private Role role = new Role();
    private Guard guard = new Guard();
    private Lobby lobby = new Lobby();
    private PrivateChat privateChat = new PrivateChat();
    private Forum forum = new Forum();
    private Follow follow = new Follow();

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
    public static class Role {
        private Boolean enabled;
        private Integer cacheSeconds;
        private Integer variantStickySeconds;
        private Double variantSwitchProbability;
    }

    @Data
    public static class Guard {
        private Boolean enabled;
        private Integer maxCallsPerHour;
        private Integer maxCallsPerDay;
    }

    @Data
    public static class Lobby {
        private Boolean enabled;
        private Long botUserId;
        private String botDisplayName;
        private Boolean idleEnabled;
        private Integer idleTriggerSeconds;
        private Double idleSpeakProbability;
        private Integer cooldownSeconds;
        private Double replyProbability;
        private Double mentionReplyProbability;
        private Integer maxRepliesPerHour;
        private Integer maxReplyChars;
        private Integer minDelaySeconds;
        private Integer maxDelaySeconds;
        private Integer mentionMinDelaySeconds;
        private Integer mentionMaxDelaySeconds;
        private String systemPrompt;
    }

    @Data
    public static class PrivateChat {
        private Boolean enabled;
        private Long botUserId;
        private Integer cooldownSeconds;
        private Double replyProbability;
        private Integer maxRepliesPerHour;
        private Integer maxReplyChars;
        private Integer minDelaySeconds;
        private Integer maxDelaySeconds;
        private String systemPrompt;
    }

    @Data
    public static class Forum {
        private Boolean enabled;
        private Long botUserId;
        private Integer cooldownSeconds;
        private Double replyProbability;
        private Double mentionReplyProbability;
        private Integer maxRepliesPerHour;
        private Integer maxRepliesPerDay;
        private Integer maxReplyChars;
        private Integer minDelaySeconds;
        private Integer maxDelaySeconds;
        private Integer mentionMinDelaySeconds;
        private Integer mentionMaxDelaySeconds;
        private Double replyToReplyProbability;
        private String systemPrompt;
    }

    @Data
    public static class Follow {
        private Boolean autoFollowBackEnabled;
        private Integer minDelaySeconds;
        private Integer maxDelaySeconds;
    }
}
