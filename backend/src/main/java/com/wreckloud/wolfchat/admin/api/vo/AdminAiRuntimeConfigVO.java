package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端 AI 运行参数视图。
 */
@Data
@Schema(description = "管理端AI运行参数")
public class AdminAiRuntimeConfigVO {
    @Schema(description = "AI总开关")
    private Boolean enabled;

    @Schema(description = "模型提供方")
    private String provider;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "是否已配置API Key")
    private Boolean apiKeyConfigured;

    @Schema(description = "采样温度")
    private Double temperature;

    @Schema(description = "最大输出Token")
    private Integer maxOutputTokens;

    @Schema(description = "超时时间毫秒")
    private Integer timeoutMs;

    @Schema(description = "全局守卫开关")
    private Boolean guardEnabled;

    @Schema(description = "全局每小时配额")
    private Integer guardMaxCallsPerHour;

    @Schema(description = "全局每日配额")
    private Integer guardMaxCallsPerDay;

    @Schema(description = "大厅AI开关")
    private Boolean lobbyEnabled;

    @Schema(description = "大厅AI用户ID")
    private Long lobbyBotUserId;

    @Schema(description = "大厅基础回复概率")
    private Double lobbyReplyProbability;

    @Schema(description = "大厅@回复概率")
    private Double lobbyMentionReplyProbability;

    @Schema(description = "大厅冷却秒数")
    private Integer lobbyCooldownSeconds;

    @Schema(description = "大厅每小时上限")
    private Integer lobbyMaxRepliesPerHour;

    @Schema(description = "大厅系统提示词")
    private String lobbySystemPrompt;

    @Schema(description = "私聊AI开关")
    private Boolean privateChatEnabled;

    @Schema(description = "私聊AI用户ID")
    private Long privateChatBotUserId;

    @Schema(description = "私聊回复概率")
    private Double privateChatReplyProbability;

    @Schema(description = "私聊冷却秒数")
    private Integer privateChatCooldownSeconds;

    @Schema(description = "私聊每小时上限")
    private Integer privateChatMaxRepliesPerHour;

    @Schema(description = "私聊系统提示词")
    private String privateChatSystemPrompt;

    @Schema(description = "论坛AI开关")
    private Boolean forumEnabled;

    @Schema(description = "论坛AI用户ID")
    private Long forumBotUserId;

    @Schema(description = "论坛基础回复概率")
    private Double forumReplyProbability;

    @Schema(description = "论坛@回复概率")
    private Double forumMentionReplyProbability;

    @Schema(description = "论坛楼中楼回复概率")
    private Double forumReplyToReplyProbability;

    @Schema(description = "论坛冷却秒数")
    private Integer forumCooldownSeconds;

    @Schema(description = "论坛每小时上限")
    private Integer forumMaxRepliesPerHour;

    @Schema(description = "论坛每日上限")
    private Integer forumMaxRepliesPerDay;

    @Schema(description = "论坛系统提示词")
    private String forumSystemPrompt;

    @Schema(description = "自动回关开关")
    private Boolean followAutoFollowBackEnabled;

    @Schema(description = "回关最小延迟秒")
    private Integer followMinDelaySeconds;

    @Schema(description = "回关最大延迟秒")
    private Integer followMaxDelaySeconds;
}

