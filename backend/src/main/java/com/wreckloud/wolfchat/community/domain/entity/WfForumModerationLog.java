package com.wreckloud.wolfchat.community.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 论坛版务日志实体
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@TableName("wf_forum_moderation_log")
public class WfForumModerationLog {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作人用户ID
     */
    private Long operatorUserId;

    /**
     * 目标类型：THREAD/REPLY
     */
    private String targetType;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 操作类型：LOCK_THREAD/UNLOCK_THREAD/DELETE_THREAD/DELETE_REPLY
     */
    private String action;

    /**
     * 操作原因
     */
    private String reason;

    private LocalDateTime createTime;
}
