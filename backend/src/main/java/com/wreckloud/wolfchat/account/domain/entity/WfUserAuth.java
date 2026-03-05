package com.wreckloud.wolfchat.account.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.account.domain.enums.UserAuthType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 用户认证实体
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Data
@TableName("wf_user_auth")
public class WfUserAuth {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 认证类型：WOLF_NO_PASSWORD/EMAIL_PASSWORD
     */
    private UserAuthType authType;

    /**
     * 认证标识（狼藉号或邮箱）
     */
    private String authIdentifier;

    /**
     * 认证凭据哈希（密码类认证使用）
     */
    private String credentialHash;

    /**
     * 是否已认证
     */
    private Boolean verified;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 最近登录时间
     */
    private LocalDateTime lastLoginAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
