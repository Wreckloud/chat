package com.wreckloud.wolfchat.follow.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.follow.domain.enums.UserBlockStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户拉黑关系
 */
@Data
@TableName("wf_user_block")
public class WfUserBlock {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 拉黑操作人
     */
    private Long blockerId;

    /**
     * 被拉黑用户
     */
    private Long blockedId;

    /**
     * 关系状态
     */
    private UserBlockStatus status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

