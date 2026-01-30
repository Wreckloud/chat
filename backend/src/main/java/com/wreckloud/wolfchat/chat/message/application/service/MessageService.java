package com.wreckloud.wolfchat.chat.message.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.chat.conversation.application.service.ConversationService;
import com.wreckloud.wolfchat.chat.conversation.domain.entity.WfConversation;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import com.wreckloud.wolfchat.chat.message.infra.mapper.WfMessageMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.follow.application.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Description 消息服务
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {
    private final WfMessageMapper messageMapper;
    private final ConversationService conversationService;
    private final FollowService followService;

    /**
     * 发送消息
     */
    @Transactional(rollbackFor = Exception.class)
    public WfMessage sendMessage(Long userId, Long conversationId, String content) {
        log.info("发送消息: userId={}, conversationId={}", userId, conversationId);
        
        // 内容校验：trim 后检查是否为空
        if (content == null || content.trim().isEmpty()) {
            log.warn("消息内容为空: userId={}", userId);
            throw new BaseException(ErrorCode.MESSAGE_CONTENT_EMPTY);
        }
        content = content.trim();

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
        message.setContent(content);
        message.setMsgType(MessageType.TEXT);
        if (message.getCreateTime() == null) {
            message.setCreateTime(java.time.LocalDateTime.now());
        }
        messageMapper.insert(message);
        log.info("消息发送成功: messageId={}, conversationId={}", message.getId(), conversationId);

        // 更新会话的最近消息信息
        conversationService.updateLastMessage(
                conversationId,
                message.getId(),
                content.length() > 100 ? content.substring(0, 100) : content,
                message.getCreateTime()
        );

        return message;
    }

    /**
     * 分页查询消息列表
     */
    public Page<WfMessage> listMessages(Long userId, Long conversationId, Integer pageNum, Integer pageSize) {
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
}

