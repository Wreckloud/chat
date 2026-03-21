package com.wreckloud.wolfchat.chat.lobby.application.event;

import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import lombok.Getter;

/**
 * 公共聊天室消息发送事件（入库成功后发布）。
 */
@Getter
public class LobbyMessageSentEvent {
    private final Long messageId;
    private final Long senderId;
    private final MessageType msgType;
    private final String content;

    public LobbyMessageSentEvent(Long messageId, Long senderId, MessageType msgType, String content) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.msgType = msgType;
        this.content = content;
    }
}

