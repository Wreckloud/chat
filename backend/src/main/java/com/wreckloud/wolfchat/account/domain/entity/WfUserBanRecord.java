package com.wreckloud.wolfchat.account.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.account.domain.enums.BanRecordStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 用户封禁记录实体
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Data
@TableName("wf_user_ban_record")
public class WfUserBanRecord {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 被封禁用户ID
     */
    private Long userId;

    /**
     * 操作人用户ID
     */
    private Long operatorUserId;

    /**
     * 封禁原因
     */
    private String reason;

    /**
     * 封禁开始时间
     */
    private LocalDateTime startTime;

    /**
     * 封禁结束时间（为空表示永久封禁）
     */
    private LocalDateTime endTime;

    /**
     * 记录状态：ACTIVE/LIFTED/EXPIRED
     */
    private BanRecordStatus status;

    /**
     * 解除时间
     */
    private LocalDateTime liftedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
