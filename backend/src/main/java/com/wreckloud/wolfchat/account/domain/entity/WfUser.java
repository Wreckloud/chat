package com.wreckloud.wolfchat.account.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
     * 登录密码（阶段1简化存储，预留哈希空间）
     */
    private String loginKey;

    /**
     * 行者名（用户的昵称）
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 状态：NORMAL-正常，DISABLED-禁用
     */
    private UserStatus status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

