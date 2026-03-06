package com.wreckloud.wolfchat.chat.message.api.converter;

import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.stream.Collectors;

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

    public static List<MessageVO> toMessageVOList(List<WfMessage> source) {
        return source.stream()
                .map(MessageConverter::toMessageVO)
                .collect(Collectors.toList());
    }

    public static Page<MessageVO> toMessageVOPage(Page<WfMessage> source) {
        Page<MessageVO> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(toMessageVOList(source.getRecords()));
        return target;
    }
}
