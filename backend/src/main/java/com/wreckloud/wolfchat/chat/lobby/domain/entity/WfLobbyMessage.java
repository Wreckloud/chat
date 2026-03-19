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
     * 客户端消息ID（用于幂等去重）
     */
    private String clientMsgId;

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
     * 视频封面对象 Key
     */
    private String mediaPosterKey;

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
     * 回复目标消息ID
     */
    private Long replyToMessageId;

    /**
     * 回复目标发送者ID
     */
    private Long replyToSenderId;

    /**
     * 回复预览文案
     */
    private String replyToPreview;

    /**
     * 发送时间
     */
    private LocalDateTime createTime;
}
