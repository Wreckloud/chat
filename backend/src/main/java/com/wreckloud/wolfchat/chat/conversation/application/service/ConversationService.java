package com.wreckloud.wolfchat.chat.conversation.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.chat.conversation.domain.entity.WfConversation;
import com.wreckloud.wolfchat.chat.conversation.infra.mapper.WfConversationMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.follow.application.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description 会话服务
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {
    private final WfConversationMapper conversationMapper;
    private final WfUserMapper userMapper;
    private final FollowService followService;

    /**
     * 获取或创建会话
     */
    @Transactional(rollbackFor = Exception.class)
    public Long getOrCreateConversation(Long userId, Long targetUserId) {
        log.info("获取或创建会话: userId={}, targetUserId={}", userId, targetUserId);
        
        // 校验互相关注
        if (!followService.isMutualFollow(userId, targetUserId)) {
            log.warn("用户未互相关注: userId={}, targetUserId={}", userId, targetUserId);
            throw new BaseException(ErrorCode.NOT_MUTUAL_FOLLOW);
        }

        // 确保 userAId < userBId
        Long userAId = Math.min(userId, targetUserId);
        Long userBId = Math.max(userId, targetUserId);

        // 查询是否已存在会话
        LambdaQueryWrapper<WfConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfConversation::getUserAId, userAId)
                .eq(WfConversation::getUserBId, userBId);
        WfConversation conversation = conversationMapper.selectOne(queryWrapper);

        if (conversation != null) {
            log.info("会话已存在: conversationId={}", conversation.getId());
            return conversation.getId();
        }

        // 创建新会话
        WfConversation newConversation = new WfConversation();
        newConversation.setUserAId(userAId);
        newConversation.setUserBId(userBId);
        try {
            conversationMapper.insert(newConversation);
            log.info("创建新会话成功: conversationId={}, userAId={}, userBId={}", 
                    newConversation.getId(), userAId, userBId);
        } catch (DuplicateKeyException ex) {
            log.warn("并发创建会话，查询已存在的会话");
            WfConversation existing = conversationMapper.selectOne(queryWrapper);
            if (existing != null) {
                return existing.getId();
            }
            throw ex;
        }

        return newConversation.getId();
    }

    /**
     * 获取会话列表
     */
    public List<WfConversation> listConversations(Long userId) {
        log.info("查询会话列表: userId={}", userId);
        LambdaQueryWrapper<WfConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.eq(WfConversation::getUserAId, userId)
                        .or()
                        .eq(WfConversation::getUserBId, userId))
                .orderByDesc(WfConversation::getLastMessageTime)
                .orderByDesc(WfConversation::getCreateTime);
        List<WfConversation> conversations = conversationMapper.selectList(queryWrapper);
        log.info("查询到会话数量: {}", conversations.size());
        return conversations;
    }

    /**
     * 更新会话的最近消息信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLastMessage(Long conversationId, Long messageId, String message, LocalDateTime time) {
        log.info("更新会话最近消息: conversationId={}, messageId={}", conversationId, messageId);
        LambdaUpdateWrapper<WfConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfConversation::getId, conversationId)
                .set(WfConversation::getLastMessageId, messageId)
                .set(WfConversation::getLastMessage, message)
                .set(WfConversation::getLastMessageTime, time);
        conversationMapper.update(null, updateWrapper);
    }

    /**
     * 获取会话信息
     */
    public WfConversation getConversation(Long conversationId) {
        WfConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            log.warn("会话不存在: conversationId={}", conversationId);
            throw new BaseException(ErrorCode.CONVERSATION_NOT_FOUND);
        }
        return conversation;
    }

    /**
     * 校验用户是否是会话参与者
     */
    public void validateConversationMember(Long conversationId, Long userId) {
        WfConversation conversation = getConversation(conversationId);
        if (!conversation.getUserAId().equals(userId) && !conversation.getUserBId().equals(userId)) {
            log.warn("用户不是会话参与者: conversationId={}, userId={}", conversationId, userId);
            throw new BaseException(ErrorCode.NOT_CONVERSATION_MEMBER);
        }
    }

    /**
     * 获取会话对方用户ID
     */
    public Long getTargetUserId(WfConversation conversation, Long currentUserId) {
        if (conversation.getUserAId().equals(currentUserId)) {
            return conversation.getUserBId();
        } else {
            return conversation.getUserAId();
        }
    }

    /**
     * 批量获取用户信息
     */
    public Map<Long, WfUser> getUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        List<WfUser> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(WfUser::getId, user -> user));
    }
}
