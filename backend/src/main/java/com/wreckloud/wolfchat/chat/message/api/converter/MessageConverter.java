package com.wreckloud.wolfchat.chat.message.api.converter;

import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;

/**
 * @Description 消息转换器
 * @Author Wreckloud
 * @Date 2026-03-02
 */
public final class MessageConverter {
    private MessageConverter() {
    }

    public static MessageVO toMessageVO(WfMessage message) {
        if (message == null) {
            return null;
        }
        MessageVO vo = new MessageVO();
        vo.setMessageId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setSenderId(message.getSenderId());
        vo.setReceiverId(message.getReceiverId());
        vo.setContent(message.getContent());
        vo.setMsgType(message.getMsgType());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
