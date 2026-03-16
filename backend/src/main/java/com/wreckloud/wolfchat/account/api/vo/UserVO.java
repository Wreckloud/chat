package com.wreckloud.wolfchat.account.api.vo;

import com.wreckloud.wolfchat.account.domain.enums.OnboardingStatus;
import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 行者信息响应 VO
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Data
@Schema(description = "行者信息")
public class UserVO {
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "狼藉号", example = "1234567890")
    private String wolfNo;

    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "邮箱是否已认证")
    private Boolean emailVerified;

    @Schema(description = "行者名（用户昵称）", example = "维克罗德")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "个性签名")
    private String signature;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "状态：NORMAL-正常，DISABLED-禁用或已注销")
    private UserStatus status;

    @Schema(description = "新用户引导状态：PENDING/COMPLETED/SKIPPED")
    private OnboardingStatus onboardingStatus;

    @Schema(description = "引导完成时间")
    private LocalDateTime onboardingCompletedAt;

    @Schema(description = "首次登录时间")
    private LocalDateTime firstLoginAt;

    @Schema(description = "最近登录时间")
    private LocalDateTime lastLoginAt;

    @Schema(description = "活跃天数（按登录日期去重统计）")
    private Integer activeDayCount;
}


