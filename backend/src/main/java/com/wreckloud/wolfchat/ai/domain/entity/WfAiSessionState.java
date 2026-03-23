package com.wreckloud.wolfchat.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 会话状态实体。
 */
@Data
@TableName("wf_ai_session_state")
public class WfAiSessionState {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String scene;

    private String sessionKey;

    private Long botUserId;

    private Long userId;

    private String mood;

    private Integer warmth;

    private Integer energy;

    private Integer patience;

    private String topic;

    private String lastUserMessage;

    private String lastAiReply;

    private Integer messageCount;

    private LocalDateTime lastTouchedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
