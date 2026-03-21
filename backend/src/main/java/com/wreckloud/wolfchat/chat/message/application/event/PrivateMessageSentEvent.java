package com.wreckloud.wolfchat.chat.message.application.event;

import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import lombok.Getter;

/**
 * 私聊消息发送事件（入库成功后发布）。
 */
@Getter
public class PrivateMessageSentEvent {
    private final Long messageId;
    private final Long conversationId;
    private final Long senderId;
    private final Long receiverId;
    private final MessageType msgType;
    private final String content;

    public PrivateMessageSentEvent(Long messageId,
                                   Long conversationId,
                                   Long senderId,
                                   Long receiverId,
                                   MessageType msgType,
                                   String content) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.msgType = msgType;
        this.content = content;
    }
}

