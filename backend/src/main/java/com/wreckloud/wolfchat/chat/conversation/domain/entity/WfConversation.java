package com.wreckloud.wolfchat.chat.conversation.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 会话实体
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Data
@TableName("wf_conversation")
public class WfConversation {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话参与者A（较小的用户ID）
     */
    private Long userAId;

    /**
     * 会话参与者B（较大的用户ID）
     */
    private Long userBId;

    /**
     * 最近一条消息ID（预留）
     */
    private Long lastMessageId;

    /**
     * 最近一条消息内容（冗余）
     */
    private String lastMessage;

    /**
     * 最近消息时间
     */
    private LocalDateTime lastMessageTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

