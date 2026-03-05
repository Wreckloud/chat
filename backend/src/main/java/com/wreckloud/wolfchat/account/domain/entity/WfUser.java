package com.wreckloud.wolfchat.account.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.account.domain.enums.OnboardingStatus;
import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 行者实体
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Data
@TableName("wf_user")
public class WfUser {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 狼藉号（唯一标识）
     */
    private String wolfNo;

    /**
     * 登录密码哈希（BCrypt）
     */
    private String loginKey;

    /**
     * 邮箱（唯一）
     */
    private String email;

    /**
     * 邮箱是否已认证
     */
    private Boolean emailVerified;

    /**
     * 行者名（用户的昵称）
     */
    @TableField(exist = false)
    private String nickname;

    /**
     * 头像URL
     */
    @TableField(exist = false)
    private String avatar;

    /**
     * 状态：NORMAL-正常，DISABLED-禁用
     */
    private UserStatus status;

    /**
     * 新用户引导状态：PENDING/COMPLETED/SKIPPED
     */
    private OnboardingStatus onboardingStatus;

    /**
     * 引导完成时间（状态为 COMPLETED 或 SKIPPED 时写入）
     */
    private LocalDateTime onboardingCompletedAt;

    /**
     * 首次登录时间
     */
    private LocalDateTime firstLoginAt;

    /**
     * 最近登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 登录次数
     */
    private Integer loginCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

