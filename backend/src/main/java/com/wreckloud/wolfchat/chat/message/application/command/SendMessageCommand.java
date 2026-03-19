package com.wreckloud.wolfchat.chat.message.application.command;

import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import lombok.Data;

/**
 * @Description 发送消息命令
 * @Author Wreckloud
 * @Date 2026-03-09
 */
@Data
public class SendMessageCommand {
    /**
     * 当前发送者 ID
     */
    private Long userId;

    /**
     * 所属会话 ID
     */
    private Long conversationId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型
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
     * 客户端消息ID（用于幂等去重）
     */
    private String clientMsgId;
}
