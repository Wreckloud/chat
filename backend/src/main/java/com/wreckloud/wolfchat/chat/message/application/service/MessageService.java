package com.wreckloud.wolfchat.chat.message.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.chat.conversation.application.service.ConversationService;
import com.wreckloud.wolfchat.chat.conversation.domain.entity.WfConversation;
import com.wreckloud.wolfchat.chat.media.application.service.ChatMediaService;
import com.wreckloud.wolfchat.chat.message.application.command.SendMessageCommand;
import com.wreckloud.wolfchat.chat.message.api.vo.MessagePolicyVO;
import com.wreckloud.wolfchat.chat.message.application.support.MessageRuleSupport;
import com.wreckloud.wolfchat.chat.message.application.event.PrivateMessageSentEvent;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageDeliveryStatus;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import com.wreckloud.wolfchat.chat.message.infra.mapper.WfMessageMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.follow.application.service.FollowService;
import com.wreckloud.wolfchat.follow.application.service.UserBlockService;
import com.wreckloud.wolfchat.notice.application.service.UserNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Description 消息服务
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {
    /**
     * 离线补发上限，避免单次补发过多导致阻塞
     */
    private static final int UNDELIVERED_LIMIT = 200;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int STRANGER_SINGLE_SIDE_LIMIT = 3;
    private static final int RECALL_WINDOW_MINUTES = 5;
    private static final int CLIENT_MSG_ID_MAX_LENGTH = 64;
    private static final String RECALL_CONTENT = "该消息已撤回";
    private static final int RECEIVER_VISIBLE = 1;
    private static final int RECEIVER_HIDDEN = 0;

    private final WfMessageMapper messageMapper;
    private final ConversationService conversationService;
    private final FollowService followService;
    private final UserBlockService userBlockService;
    private final ChatMediaService chatMediaService;
    private final MessageMediaService messageMediaService;
    private final ChatSystemNoticeService chatSystemNoticeService;
    private final UserNoticeService userNoticeService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 发送消息
     */
    @Transactional(rollbackFor = Exception.class)
    public WfMessage sendMessage(SendMessageCommand command) {
        Long userId = command.getUserId();
        Long conversationId = command.getConversationId();
        log.debug("发送消息: userId={}, conversationId={}", userId, conversationId);

        MessageType msgType = MessageRuleSupport.normalizeMessageType(command.getMsgType());
        String normalizedContent = MessageRuleSupport.normalizeContent(command.getContent(), msgType);
        command.setMsgType(msgType);

        // 校验会话存在且用户是参与者
        conversationService.validateConversationMember(conversationId, userId);

        // 获取会话信息，确定接收者
        WfConversation conversation = conversationService.getConversation(conversationId);
        Long receiverId = conversationService.getTargetUserId(conversation, userId);

        String clientMsgId = normalizeClientMsgId(command.getClientMsgId());
        command.setClientMsgId(clientMsgId);
        WfMessage existingMessage = findExistingMessageByClientMsgId(userId, conversationId, clientMsgId);
        if (existingMessage != null) {
            return existingMessage;
        }

        boolean blockedByReceiver = userBlockService.isBlocked(receiverId, userId);
        boolean mutualFollow = followService.isMutualFollow(userId, receiverId);
        int sentCountBefore = blockedByReceiver
                ? 0
                : validateSendPermission(conversationId, userId, receiverId, mutualFollow);

        chatMediaService.validateMessagePayload(userId, command);
        WfMessage replyToMessage = resolveReplyTargetMessage(conversationId, command.getReplyToMessageId());

        // 保存消息
        WfMessage message = new WfMessage();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setClientMsgId(clientMsgId);
        message.setReceiverId(receiverId);
        message.setContent(normalizedContent);
        message.setMsgType(msgType);
        message.setMediaKey(command.getMediaKey());
        message.setMediaPosterKey(command.getMediaPosterKey());
        message.setMediaWidth(command.getMediaWidth());
        message.setMediaHeight(command.getMediaHeight());
        message.setMediaSize(command.getMediaSize());
        message.setMediaMimeType(command.getMediaMimeType());
        if (replyToMessage != null) {
            message.setReplyToMessageId(replyToMessage.getId());
            message.setReplyToSenderId(replyToMessage.getSenderId());
            message.setReplyToPreview(messageMediaService.buildReplyPreview(replyToMessage.getMsgType(), replyToMessage.getContent()));
        }
        message.setReceiverVisible(blockedByReceiver ? RECEIVER_HIDDEN : RECEIVER_VISIBLE);
        message.setDelivered(blockedByReceiver ? MessageDeliveryStatus.FAILED : MessageDeliveryStatus.UNDELIVERED);
        message.setCreateTime(LocalDateTime.now());
        try {
            int insertRows = messageMapper.insert(message);
            if (insertRows != 1) {
                throw new BaseException(ErrorCode.DATABASE_ERROR);
            }
        } catch (DuplicateKeyException ex) {
            WfMessage duplicatedMessage = findExistingMessageByClientMsgId(userId, conversationId, clientMsgId);
            if (duplicatedMessage != null) {
                return duplicatedMessage;
            }
            throw ex;
        }
        log.debug("消息发送成功: messageId={}, conversationId={}", message.getId(), conversationId);

        if (blockedByReceiver) {
            chatSystemNoticeService.sendBlockRejectedNoticeDaily(conversationId, userId);
            return message;
        }

        // 更新会话的最近消息信息
        conversationService.updateLastMessage(
                conversationId,
                message.getId(),
                messageMediaService.buildConversationPreview(msgType, normalizedContent),
                message.getCreateTime()
        );
        conversationService.increaseUnreadCount(conversationId, receiverId);
        if (!mutualFollow && sentCountBefore == 0) {
            chatSystemNoticeService.sendStrangerRuleNotice(conversationId, userId, STRANGER_SINGLE_SIDE_LIMIT);
        }
        if (replyToMessage != null) {
            userNoticeService.notifyChatMessageReplied(
                    replyToMessage.getSenderId(),
                    conversationId,
                    userId
            );
        }
        applicationEventPublisher.publishEvent(new PrivateMessageSentEvent(
                message.getId(),
                conversationId,
                userId,
                receiverId,
                message.getMsgType(),
                message.getContent()
        ));

        return message;
    }

    /**
     * 分页查询消息列表
     */
    public Page<WfMessage> listMessages(Long userId, Long conversationId, Integer pageNum, Integer pageSize) {
        MessageRuleSupport.validatePageParams(pageNum, pageSize, MAX_PAGE_SIZE);
        log.debug("查询消息列表: userId={}, conversationId={}, page={}, size={}",
                userId, conversationId, pageNum, pageSize);
        
        // 校验会话存在且用户是参与者
        conversationService.validateConversationMember(conversationId, userId);

        // 分页查询消息
        Page<WfMessage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WfMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfMessage::getConversationId, conversationId)
                .and(visible -> visible.eq(WfMessage::getSenderId, userId)
                        .or(receiver -> receiver.eq(WfMessage::getReceiverId, userId)
                                .eq(WfMessage::getReceiverVisible, RECEIVER_VISIBLE)))
                .and(wrapper -> wrapper.ne(WfMessage::getMsgType, MessageType.SYSTEM)
                        .or(system -> system.eq(WfMessage::getMsgType, MessageType.SYSTEM)
                                .eq(WfMessage::getReceiverId, userId)))
                .orderByDesc(WfMessage::getCreateTime)
                .orderByDesc(WfMessage::getId);

        Page<WfMessage> result = messageMapper.selectPage(page, queryWrapper);
        log.debug("查询到消息数量: {}, 总数: {}", result.getRecords().size(), result.getTotal());
        return result;
    }

    public WfMessage getById(Long messageId) {
        if (messageId == null || messageId <= 0L) {
            return null;
        }
        return messageMapper.selectById(messageId);
    }

    /**
     * 查询当前会话的消息策略
     */
    public MessagePolicyVO getMessagePolicy(Long userId, Long conversationId) {
        conversationService.validateConversationMember(conversationId, userId);
        WfConversation conversation = conversationService.getConversation(conversationId);
        Long targetUserId = conversationService.getTargetUserId(conversation, userId);

        boolean mutualFollow = followService.isMutualFollow(userId, targetUserId);
        MessagePolicySnapshot policySnapshot = buildPolicySnapshot(conversationId, userId, targetUserId, mutualFollow);

        MessagePolicyVO policyVO = new MessagePolicyVO();
        policyVO.setConversationId(conversationId);
        policyVO.setMutualFollow(policySnapshot.mutualFollow);
        policyVO.setInteractionUnlocked(policySnapshot.interactionUnlocked);
        policyVO.setCanSendFreely(policySnapshot.canSendFreely);
        policyVO.setStrangerMessageLimit(STRANGER_SINGLE_SIDE_LIMIT);
        policyVO.setStrangerMessageSent(policySnapshot.sentCount);
        policyVO.setStrangerMessageRemaining(policySnapshot.remaining);
        return policyVO;
    }

    /**
     * 查询未送达消息
     */
    public List<WfMessage> listUndeliveredMessages(Long userId) {
        LambdaQueryWrapper<WfMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfMessage::getReceiverId, userId)
                .eq(WfMessage::getDelivered, MessageDeliveryStatus.UNDELIVERED)
                .orderByAsc(WfMessage::getCreateTime)
                .orderByAsc(WfMessage::getId)
                .last("LIMIT " + UNDELIVERED_LIMIT);
        return messageMapper.selectList(queryWrapper);
    }

    /**
     * 标记消息为已送达
     */
    @Transactional(rollbackFor = Exception.class)
    public void markDelivered(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        WfMessage update = new WfMessage();
        update.setDelivered(MessageDeliveryStatus.DELIVERED);
        update.setDeliveredTime(LocalDateTime.now());
        int updateRows = messageMapper.update(update, new LambdaQueryWrapper<WfMessage>()
                .eq(WfMessage::getDelivered, MessageDeliveryStatus.UNDELIVERED)
                .in(WfMessage::getId, messageIds));
        if (updateRows == 0) {
            log.debug("标记消息送达未命中: messageIds={}", messageIds);
        }
    }

    /**
     * 撤回消息（仅发送者在 5 分钟内可撤回）
     */
    @Transactional(rollbackFor = Exception.class)
    public WfMessage recallMessage(Long userId, Long conversationId, Long messageId) {
        if (messageId == null || messageId <= 0L) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "消息ID不能为空");
        }
        conversationService.validateConversationMember(conversationId, userId);

        WfMessage message = messageMapper.selectById(messageId);
        if (message == null || !conversationId.equals(message.getConversationId())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "消息不存在");
        }
        if (!userId.equals(message.getSenderId())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "只能撤回自己发送的消息");
        }
        if (MessageType.RECALL.equals(message.getMsgType())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "消息已撤回");
        }

        LocalDateTime createTime = message.getCreateTime();
        if (createTime == null || LocalDateTime.now().isAfter(createTime.plusMinutes(RECALL_WINDOW_MINUTES))) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "消息超过5分钟，无法撤回");
        }

        message.setMsgType(MessageType.RECALL);
        message.setContent(RECALL_CONTENT);
        message.setMediaKey(null);
        message.setMediaPosterKey(null);
        message.setMediaWidth(null);
        message.setMediaHeight(null);
        message.setMediaSize(null);
        message.setMediaMimeType(null);
        message.setReplyToMessageId(null);
        message.setReplyToSenderId(null);
        message.setReplyToPreview(null);
        int updateRows = messageMapper.updateById(message);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }

        WfConversation conversation = conversationService.getConversation(conversationId);
        if (messageId.equals(conversation.getLastMessageId())) {
            conversationService.updateLastMessage(
                    conversationId,
                    message.getId(),
                    messageMediaService.buildConversationPreview(message.getMsgType(), message.getContent()),
                    message.getCreateTime()
            );
        }
        return messageMapper.selectById(messageId);
    }

    private int validateSendPermission(Long conversationId, Long senderId, Long targetUserId, boolean mutualFollow) {
        if (mutualFollow) {
            return 0;
        }
        int sentCount = countSenderMessagesSinceLastTargetReply(conversationId, senderId, targetUserId);
        if (sentCount >= STRANGER_SINGLE_SIDE_LIMIT) {
            throw new BaseException(ErrorCode.CHAT_STRANGER_MESSAGE_LIMIT);
        }
        return sentCount;
    }

    private MessagePolicySnapshot buildPolicySnapshot(Long conversationId, Long senderId, Long targetUserId, boolean mutualFollow) {
        boolean interactionUnlocked = mutualFollow;
        boolean canSendFreely = mutualFollow;
        int sentCount = canSendFreely ? 0 : countSenderMessagesSinceLastTargetReply(conversationId, senderId, targetUserId);
        int remaining = canSendFreely
                ? STRANGER_SINGLE_SIDE_LIMIT
                : Math.max(0, STRANGER_SINGLE_SIDE_LIMIT - sentCount);
        return new MessagePolicySnapshot(mutualFollow, interactionUnlocked, canSendFreely, sentCount, remaining);
    }

    private int countSenderMessagesSinceLastTargetReply(Long conversationId, Long senderId, Long targetUserId) {
        Long lastTargetMessageId = messageMapper.selectLastMessageIdByConversationAndSender(conversationId, targetUserId);
        Long count = messageMapper.countByConversationAndSenderAfterMessageId(conversationId, senderId, lastTargetMessageId);
        if (count == null || count <= 0L) {
            return 0;
        }
        if (count > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return count.intValue();
    }

    private WfMessage resolveReplyTargetMessage(Long conversationId, Long replyToMessageId) {
        if (replyToMessageId == null || replyToMessageId <= 0L) {
            return null;
        }
        WfMessage replyToMessage = messageMapper.selectById(replyToMessageId);
        if (replyToMessage == null || !conversationId.equals(replyToMessage.getConversationId())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "回复目标消息不存在");
        }
        return replyToMessage;
    }

    private String normalizeClientMsgId(String clientMsgId) {
        if (!StringUtils.hasText(clientMsgId)) {
            return null;
        }
        String normalized = clientMsgId.trim();
        if (normalized.length() > CLIENT_MSG_ID_MAX_LENGTH) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "消息标识长度非法");
        }
        return normalized;
    }

    private WfMessage findExistingMessageByClientMsgId(Long senderId, Long conversationId, String clientMsgId) {
        if (senderId == null || senderId <= 0L
                || conversationId == null || conversationId <= 0L
                || !StringUtils.hasText(clientMsgId)) {
            return null;
        }
        return messageMapper.selectBySenderAndConversationAndClientMsgId(senderId, conversationId, clientMsgId);
    }

    private static class MessagePolicySnapshot {
        private final boolean mutualFollow;
        private final boolean interactionUnlocked;
        private final boolean canSendFreely;
        private final int sentCount;
        private final int remaining;

        private MessagePolicySnapshot(boolean mutualFollow, boolean interactionUnlocked, boolean canSendFreely, int sentCount, int remaining) {
            this.mutualFollow = mutualFollow;
            this.interactionUnlocked = interactionUnlocked;
            this.canSendFreely = canSendFreely;
            this.sentCount = sentCount;
            this.remaining = remaining;
        }
    }

}

