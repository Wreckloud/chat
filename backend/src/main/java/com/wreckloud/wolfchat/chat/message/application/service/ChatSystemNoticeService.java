package com.wreckloud.wolfchat.chat.message.application.service;

import com.alibaba.fastjson.JSON;
import com.wreckloud.wolfchat.chat.message.api.converter.MessageConverter;
import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageDeliveryStatus;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import com.wreckloud.wolfchat.chat.message.infra.mapper.WfMessageMapper;
import com.wreckloud.wolfchat.chat.websocket.dto.WsResponse;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.chat.websocket.session.WsSessionManager;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 聊天系统提示消息服务（入会话时间线）。
 */
@Service
@RequiredArgsConstructor
public class ChatSystemNoticeService {
    private static final Long SYSTEM_SENDER_ID = 0L;
    private static final int RECEIVER_VISIBLE = 1;
    private static final String STRANGER_RULE_TEMPLATE = "未互关时，单方连续最多发送 %d 条消息，待对方回复后可继续发送。";
    private static final String FOLLOW_RECEIVED_TEXT = "对方关注了你";
    private static final String MUTUAL_FOLLOW_TEXT = "你们已经是好友了";
    private static final String BLOCK_REJECTED_TEXT = "对方拒绝接收你的消息";

    private final WfMessageMapper wfMessageMapper;
    private final MessageMediaService messageMediaService;
    private final WsSessionManager wsSessionManager;

    /**
     * 首次进入陌生人连续发送窗口时，给发送方写入规则提示。
     */
    public void sendStrangerRuleNotice(Long conversationId, Long receiverUserId, int limit) {
        if (limit <= 0) {
            return;
        }
        String content = String.format(STRANGER_RULE_TEMPLATE, limit);
        insertAndPush(conversationId, receiverUserId, content);
    }

    /**
     * 对方关注你。
     */
    public void sendFollowReceivedNotice(Long conversationId, Long receiverUserId) {
        insertAndPush(conversationId, receiverUserId, FOLLOW_RECEIVED_TEXT);
    }

    /**
     * 互关达成后给双方写入同一条确认提示。
     */
    public void sendMutualFollowNotice(Long conversationId, Long userAId, Long userBId) {
        if (Objects.equals(userAId, userBId)) {
            return;
        }
        insertAndPush(conversationId, userAId, MUTUAL_FOLLOW_TEXT);
        insertAndPush(conversationId, userBId, MUTUAL_FOLLOW_TEXT);
    }

    /**
     * 被对方拒收时给发送方写入提示（自然日内仅一次）。
     */
    public void sendBlockRejectedNoticeDaily(Long conversationId, Long receiverUserId) {
        if (conversationId == null || conversationId <= 0L || receiverUserId == null || receiverUserId <= 0L) {
            return;
        }
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        Long count = wfMessageMapper.countSystemNoticeSince(conversationId, receiverUserId, BLOCK_REJECTED_TEXT, dayStart);
        if (count != null && count > 0L) {
            return;
        }
        insertAndPush(conversationId, receiverUserId, BLOCK_REJECTED_TEXT);
    }

    private void insertAndPush(Long conversationId, Long receiverUserId, String content) {
        if (conversationId == null || conversationId <= 0L || receiverUserId == null || receiverUserId <= 0L) {
            return;
        }
        if (!StringUtils.hasText(content)) {
            return;
        }
        WfMessage notice = new WfMessage();
        notice.setConversationId(conversationId);
        notice.setSenderId(SYSTEM_SENDER_ID);
        notice.setReceiverId(receiverUserId);
        notice.setContent(content.trim());
        notice.setMsgType(MessageType.SYSTEM);
        notice.setReceiverVisible(RECEIVER_VISIBLE);
        notice.setDelivered(MessageDeliveryStatus.UNDELIVERED);
        notice.setCreateTime(LocalDateTime.now());
        int insertRows = wfMessageMapper.insert(notice);
        if (insertRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        MessageVO messageVO = MessageConverter.toMessageVO(notice, null);
        messageMediaService.fillMedia(messageVO);
        WsResponse response = new WsResponse();
        response.setType(WsType.MESSAGE);
        response.setData(messageVO);
        int success = wsSessionManager.sendToUser(receiverUserId, JSON.toJSONString(response));
        if (success > 0) {
            WfMessage delivered = new WfMessage();
            delivered.setId(notice.getId());
            delivered.setDelivered(MessageDeliveryStatus.DELIVERED);
            delivered.setDeliveredTime(LocalDateTime.now());
            wfMessageMapper.updateById(delivered);
        }
    }
}
