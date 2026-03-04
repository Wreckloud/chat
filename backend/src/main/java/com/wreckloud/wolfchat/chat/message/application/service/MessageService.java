package com.wreckloud.wolfchat.chat.message.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.chat.conversation.application.service.ConversationService;
import com.wreckloud.wolfchat.chat.conversation.domain.entity.WfConversation;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageDeliveryStatus;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import com.wreckloud.wolfchat.chat.message.infra.mapper.WfMessageMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.follow.application.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final WfMessageMapper messageMapper;
    private final ConversationService conversationService;
    private final FollowService followService;

    /**
     * 发送消息
     */
    @Transactional(rollbackFor = Exception.class)
    public WfMessage sendMessage(Long userId, Long conversationId, String content) {
        log.info("发送消息: userId={}, conversationId={}", userId, conversationId);

        String normalizedContent = normalizeContent(content, userId);

        // 校验会话存在且用户是参与者
        conversationService.validateConversationMember(conversationId, userId);

        // 获取会话信息，确定接收者
        WfConversation conversation = conversationService.getConversation(conversationId);
        Long receiverId = conversationService.getTargetUserId(conversation, userId);

        // 校验互相关注
        if (!followService.isMutualFollow(userId, receiverId)) {
            log.warn("用户未互相关注: senderId={}, receiverId={}", userId, receiverId);
            throw new BaseException(ErrorCode.NOT_MUTUAL_FOLLOW);
        }

        // 保存消息
        WfMessage message = new WfMessage();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setReceiverId(receiverId);
        message.setContent(normalizedContent);
        message.setMsgType(MessageType.TEXT);
        message.setDelivered(MessageDeliveryStatus.UNDELIVERED);
        message.setCreateTime(LocalDateTime.now());
        int insertRows = messageMapper.insert(message);
        if (insertRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
        log.info("消息发送成功: messageId={}, conversationId={}", message.getId(), conversationId);

        // 更新会话的最近消息信息
        conversationService.updateLastMessage(
                conversationId,
                message.getId(),
                normalizedContent.length() > 100 ? normalizedContent.substring(0, 100) : normalizedContent,
                message.getCreateTime()
        );

        return message;
    }

    /**
     * 分页查询消息列表
     */
    public Page<WfMessage> listMessages(Long userId, Long conversationId, Integer pageNum, Integer pageSize) {
        validatePageParams(pageNum, pageSize);
        log.info("查询消息列表: userId={}, conversationId={}, page={}, size={}", 
                userId, conversationId, pageNum, pageSize);
        
        // 校验会话存在且用户是参与者
        conversationService.validateConversationMember(conversationId, userId);

        // 分页查询消息
        Page<WfMessage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WfMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfMessage::getConversationId, conversationId)
                .orderByDesc(WfMessage::getCreateTime);

        Page<WfMessage> result = messageMapper.selectPage(page, queryWrapper);
        log.info("查询到消息数量: {}, 总数: {}", result.getRecords().size(), result.getTotal());
        return result;
    }

    /**
     * 查询未送达消息
     */
    public List<WfMessage> listUndeliveredMessages(Long userId) {
        LambdaQueryWrapper<WfMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfMessage::getReceiverId, userId)
                .eq(WfMessage::getDelivered, MessageDeliveryStatus.UNDELIVERED)
                .orderByAsc(WfMessage::getCreateTime)
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
                .in(WfMessage::getId, messageIds));
        if (updateRows == 0) {
            log.warn("标记消息送达未命中: messageIds={}", messageIds);
        }
    }

    private String normalizeContent(String content, Long userId) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("消息内容为空: userId={}", userId);
            throw new BaseException(ErrorCode.MESSAGE_CONTENT_EMPTY);
        }
        return content.trim();
    }

    private void validatePageParams(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1 || pageSize == null || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
    }
}

