package com.wreckloud.wolfchat.chat.lobby.application.command;

import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import lombok.Data;

/**
 * @Description 发送大厅消息命令
 * @Author Wreckloud
 * @Date 2026-03-12
 */
@Data
public class SendLobbyMessageCommand {
    /**
     * 发送者ID
     */
    private Long userId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型
     */
    private MessageType msgType;

    /**
     * 媒体对象Key
     */
    private String mediaKey;

    /**
     * 视频封面对象Key
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
