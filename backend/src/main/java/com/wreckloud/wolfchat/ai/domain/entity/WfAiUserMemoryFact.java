package com.wreckloud.wolfchat.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 用户长期记忆事实。
 */
@Data
@TableName("wf_ai_user_memory_fact")
public class WfAiUserMemoryFact {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long botUserId;

    private Long userId;

    private String factKey;

    private String factValue;

    private Double confidence;

    private String sourceScene;

    private LocalDateTime lastSeenAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
