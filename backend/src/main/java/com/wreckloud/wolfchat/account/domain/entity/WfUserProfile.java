package com.wreckloud.wolfchat.account.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 用户资料实体
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Data
@TableName("wf_user_profile")
public class WfUserProfile {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（唯一）
     */
    private Long userId;

    /**
     * 行者名（公开显示）
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 个性签名
     */
    private String signature;

    /**
     * 个人简介
     */
    private String bio;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
