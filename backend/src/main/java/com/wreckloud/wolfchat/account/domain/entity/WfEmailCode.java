package com.wreckloud.wolfchat.account.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 邮箱验证码实体
 * @Author Wreckloud
 * @Date 2026-03-04
 */
@Data
@TableName("wf_email_code")
public class WfEmailCode {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 目标邮箱
     */
    private String email;

    /**
     * 验证场景（BIND_EMAIL/RESET_PASSWORD）
     */
    private String scene;

    /**
     * 验证码
     */
    private String verifyCode;

    /**
     * 是否已使用
     */
    private Boolean used;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 使用时间
     */
    private LocalDateTime usedTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
