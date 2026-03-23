package com.wreckloud.wolfchat.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 会话摘要实体。
 */
@Data
@TableName("wf_ai_session_summary")
public class WfAiSessionSummary {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String scene;

    private String sessionKey;

    private Long botUserId;

    private String summaryText;

    private Integer messageCount;

    private LocalDateTime lastSummarizedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
