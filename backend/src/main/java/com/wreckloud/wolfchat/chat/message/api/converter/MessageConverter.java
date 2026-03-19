package com.wreckloud.wolfchat.chat.message.api.converter;

import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description 消息转换器
 * @Author Wreckloud
 * @Date 2026-03-02
 */
public final class MessageConverter {
    private MessageConverter() {
    }

    public static MessageVO toMessageVO(WfMessage message, WfUser sender) {
        if (message == null) {
            return null;
        }
        MessageVO vo = new MessageVO();
        vo.setMessageId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setSenderId(message.getSenderId());
        if (sender != null) {
            vo.setSenderWolfNo(sender.getWolfNo());
            vo.setSenderNickname(sender.getNickname());
            vo.setSenderEquippedTitleName(sender.getEquippedTitleName());
            vo.setSenderEquippedTitleColor(sender.getEquippedTitleColor());
            vo.setSenderAvatar(sender.getAvatar());
        }
        vo.setReceiverId(message.getReceiverId());
        vo.setContent(message.getContent());
        vo.setMsgType(message.getMsgType());
        vo.setMediaKey(message.getMediaKey());
        vo.setMediaPosterKey(message.getMediaPosterKey());
        vo.setMediaWidth(message.getMediaWidth());
        vo.setMediaHeight(message.getMediaHeight());
        vo.setMediaSize(message.getMediaSize());
        vo.setMediaMimeType(message.getMediaMimeType());
        vo.setReplyToMessageId(message.getReplyToMessageId());
        vo.setReplyToSenderId(message.getReplyToSenderId());
        vo.setReplyToPreview(message.getReplyToPreview());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    public static List<MessageVO> toMessageVOList(List<WfMessage> source, Map<Long, WfUser> senderMap) {
        return source.stream()
                .map(item -> toMessageVO(item, senderMap.get(item.getSenderId())))
                .collect(Collectors.toList());
    }

    public static Page<MessageVO> toMessageVOPage(Page<WfMessage> source, Map<Long, WfUser> senderMap) {
        Page<MessageVO> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(toMessageVOList(source.getRecords(), senderMap));
        return target;
    }
}
