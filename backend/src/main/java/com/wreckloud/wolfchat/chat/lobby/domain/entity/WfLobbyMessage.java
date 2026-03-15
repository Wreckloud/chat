package com.wreckloud.wolfchat.chat.lobby.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 大厅消息实体
 * @Author Wreckloud
 * @Date 2026-03-12
 */
@Data
@TableName("wf_lobby_message")
public class WfLobbyMessage {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型：TEXT/IMAGE/VIDEO/FILE
     */
    private MessageType msgType;

    /**
     * 媒体对象 Key
     */
    private String mediaKey;

    /**
     * 媒体宽度
     */
    private Integer mediaWidth;

    /**
     * 媒体高度
     */
    private Integer mediaHeight;

    /**
     * 媒体大小
     */
    private Long mediaSize;

    /**
     * 媒体 MIME 类型
     */
    private String mediaMimeType;

    /**
     * 发送时间
     */
    private LocalDateTime createTime;
}

