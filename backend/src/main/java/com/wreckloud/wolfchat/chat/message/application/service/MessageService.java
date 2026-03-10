package com.wreckloud.wolfchat.chat.message.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.chat.conversation.application.service.ConversationService;
import com.wreckloud.wolfchat.chat.conversation.domain.entity.WfConversation;
import com.wreckloud.wolfchat.chat.media.application.service.ChatMediaService;
import com.wreckloud.wolfchat.chat.message.application.command.SendMessageCommand;
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

    private final WfMessageMapper messageMapper;
    private final ConversationService conversationService;
    private final FollowService followService;
    private final ChatMediaService chatMediaService;
    private final MessageMediaService messageMediaService;

    /**
     * 发送消息
     */
    @Transactional(rollbackFor = Exception.class)
    public WfMessage sendMessage(SendMessageCommand command) {
        Long userId = command.getUserId();
        Long conversationId = command.getConversationId();
        log.info("发送消息: userId={}, conversationId={}", userId, conversationId);

        MessageType msgType = normalizeMessageType(command.getMsgType());
        String normalizedContent = normalizeContent(command.getContent(), msgType);
        command.setMsgType(msgType);

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

        switch (msgType) {
            case TEXT:
                validateNoMediaFields(command);
                break;
            case IMAGE:
                chatMediaService.validateImageMessage(userId, command);
                break;
            case VIDEO:
                chatMediaService.validateVideoMessage(userId, command);
                break;
            case FILE:
                chatMediaService.validateFileMessage(userId, command);
                break;
            default:
                throw new BaseException(ErrorCode.PARAM_ERROR, "消息类型不支持");
        }

        // 保存消息
        WfMessage message = new WfMessage();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setReceiverId(receiverId);
        message.setContent(normalizedContent);
        message.setMsgType(msgType);
        message.setMediaKey(command.getMediaKey());
        message.setMediaWidth(command.getMediaWidth());
        message.setMediaHeight(command.getMediaHeight());
        message.setMediaSize(command.getMediaSize());
        message.setMediaMimeType(command.getMediaMimeType());
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
                messageMediaService.buildConversationPreview(msgType, normalizedContent),
                message.getCreateTime()
        );
        conversationService.increaseUnreadCount(conversationId, receiverId);

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
                .orderByDesc(WfMessage::getCreateTime)
                .orderByDesc(WfMessage::getId);

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
            log.warn("标记消息送达未命中: messageIds={}", messageIds);
        }
    }

    private MessageType normalizeMessageType(MessageType msgType) {
        return msgType == null ? MessageType.TEXT : msgType;
    }

    private String normalizeContent(String content, MessageType msgType) {
        if (MessageType.TEXT.equals(msgType)) {
            if (!StringUtils.hasText(content)) {
                throw new BaseException(ErrorCode.MESSAGE_CONTENT_EMPTY);
            }
            return content.trim();
        }

        if (MessageType.FILE.equals(msgType)) {
            if (!StringUtils.hasText(content)) {
                throw new BaseException(ErrorCode.PARAM_ERROR, "文件名称不能为空");
            }
            String normalizedFileName = content.trim();
            if (normalizedFileName.length() > 120) {
                throw new BaseException(ErrorCode.PARAM_ERROR, "文件名称不能超过 120 个字符");
            }
            return normalizedFileName;
        }

        if (!StringUtils.hasText(content)) {
            return null;
        }
        String normalized = content.trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.length() > 500) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "图片说明不能超过 500 个字符");
        }
        return normalized;
    }

    private void validatePageParams(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1 || pageSize == null || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
    }

    private void validateNoMediaFields(SendMessageCommand command) {
        if (StringUtils.hasText(command.getMediaKey())
                || command.getMediaWidth() != null
                || command.getMediaHeight() != null
                || command.getMediaSize() != null
                || StringUtils.hasText(command.getMediaMimeType())) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "文本消息不支持媒体字段");
        }
    }
}

