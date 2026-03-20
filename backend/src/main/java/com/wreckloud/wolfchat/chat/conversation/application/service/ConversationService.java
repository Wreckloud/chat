package com.wreckloud.wolfchat.chat.conversation.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.chat.conversation.api.vo.ConversationVO;
import com.wreckloud.wolfchat.chat.conversation.domain.entity.WfConversation;
import com.wreckloud.wolfchat.chat.conversation.infra.mapper.WfConversationMapper;
import com.wreckloud.wolfchat.chat.presence.application.service.UserPresenceService;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private static final int MARK_UNREAD_COUNT = 1;

    private final WfConversationMapper conversationMapper;
    private final UserService userService;
    private final UserPresenceService userPresenceService;

    /**
     * 获取或创建会话
     */
    @Transactional(rollbackFor = Exception.class)
    public Long getOrCreateConversation(Long userId, Long targetUserId) {
        log.debug("获取或创建会话: userId={}, targetUserId={}", userId, targetUserId);

        if (userId.equals(targetUserId)) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "不能与自己发起聊天");
        }
        userService.getEnabledByIdOrThrow(targetUserId);

        // 确保 userAId < userBId
        Long userAId = Math.min(userId, targetUserId);
        Long userBId = Math.max(userId, targetUserId);

        // 查询是否已存在会话
        LambdaQueryWrapper<WfConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfConversation::getUserAId, userAId)
                .eq(WfConversation::getUserBId, userBId);
        WfConversation conversation = conversationMapper.selectOne(queryWrapper);

        if (conversation != null) {
            log.debug("会话已存在: conversationId={}", conversation.getId());
            return conversation.getId();
        }

        // 创建新会话
        WfConversation newConversation = new WfConversation();
        newConversation.setUserAId(userAId);
        newConversation.setUserBId(userBId);
        newConversation.setUserAUnreadCount(0);
        newConversation.setUserBUnreadCount(0);
        try {
            int insertRows = conversationMapper.insert(newConversation);
            if (insertRows != 1) {
                throw new BaseException(ErrorCode.DATABASE_ERROR);
            }
            log.debug("创建新会话成功: conversationId={}, userAId={}, userBId={}",
                    newConversation.getId(), userAId, userBId);
        } catch (DuplicateKeyException ex) {
            log.warn("并发创建会话，查询已存在会话: userAId={}, userBId={}", userAId, userBId);
            WfConversation existing = conversationMapper.selectOne(queryWrapper);
            if (existing != null) {
                return existing.getId();
            }
            throw ex;
        }

        return newConversation.getId();
    }

    /**
     * 按双方用户ID查询已存在会话，不存在返回 null。
     */
    public Long findConversationId(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null || userId.equals(targetUserId)) {
            return null;
        }
        Long userAId = Math.min(userId, targetUserId);
        Long userBId = Math.max(userId, targetUserId);
        LambdaQueryWrapper<WfConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfConversation::getUserAId, userAId)
                .eq(WfConversation::getUserBId, userBId)
                .select(WfConversation::getId)
                .last("LIMIT 1");
        WfConversation conversation = conversationMapper.selectOne(queryWrapper);
        return conversation == null ? null : conversation.getId();
    }

    /**
     * 获取会话列表
     */
    public List<WfConversation> listConversations(Long userId) {
        log.debug("查询会话列表: userId={}", userId);
        LambdaQueryWrapper<WfConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.eq(WfConversation::getUserAId, userId)
                        .or()
                        .eq(WfConversation::getUserBId, userId))
                .orderByDesc(WfConversation::getLastMessageTime)
                .orderByDesc(WfConversation::getCreateTime);
        List<WfConversation> conversations = conversationMapper.selectList(queryWrapper);
        log.debug("查询到会话数量: {}", conversations.size());
        return conversations;
    }

    /**
     * 更新会话的最近消息信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLastMessage(Long conversationId, Long messageId, String message, LocalDateTime time) {
        log.debug("更新会话最近消息: conversationId={}, messageId={}", conversationId, messageId);
        int updateRows = conversationMapper.updateLastMessageIfNewer(conversationId, messageId, message, time);
        if (updateRows == 1) {
            return;
        }

        WfConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BaseException(ErrorCode.CONVERSATION_NOT_FOUND);
        }
        log.debug("跳过会话最近消息覆盖: conversationId={}, incomingMessageId={}, currentLastMessageId={}",
                conversationId, messageId, conversation.getLastMessageId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void increaseUnreadCount(Long conversationId, Long receiverId) {
        WfConversation conversation = getConversation(conversationId);
        boolean userA = isUserA(conversation, receiverId);

        LambdaUpdateWrapper<WfConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfConversation::getId, conversationId);
        if (userA) {
            updateWrapper.setSql("user_a_unread_count = user_a_unread_count + 1");
        } else {
            updateWrapper.setSql("user_b_unread_count = user_b_unread_count + 1");
        }
        int updateRows = conversationMapper.update(null, updateWrapper);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void markConversationRead(Long conversationId, Long userId) {
        WfConversation conversation = getConversation(conversationId);
        boolean userA = isUserA(conversation, userId);

        LambdaUpdateWrapper<WfConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfConversation::getId, conversationId);
        if (userA) {
            updateWrapper.set(WfConversation::getUserAUnreadCount, 0);
        } else {
            updateWrapper.set(WfConversation::getUserBUnreadCount, 0);
        }
        int updateRows = conversationMapper.update(null, updateWrapper);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void markConversationUnread(Long conversationId, Long userId) {
        WfConversation conversation = getConversation(conversationId);
        boolean userA = isUserA(conversation, userId);

        LambdaUpdateWrapper<WfConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfConversation::getId, conversationId);
        if (userA) {
            updateWrapper.set(WfConversation::getUserAUnreadCount, MARK_UNREAD_COUNT);
        } else {
            updateWrapper.set(WfConversation::getUserBUnreadCount, MARK_UNREAD_COUNT);
        }
        int updateRows = conversationMapper.update(null, updateWrapper);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
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
        }
        if (conversation.getUserBId().equals(currentUserId)) {
            return conversation.getUserAId();
        }
        throw new BaseException(ErrorCode.NOT_CONVERSATION_MEMBER);
    }

    /**
     * 获取当前用户会话列表（VO）
     */
    public List<ConversationVO> listConversationVOs(Long userId) {
        List<WfConversation> conversations = listConversations(userId);
        if (conversations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> targetUserIds = conversations.stream()
                .map(conversation -> getTargetUserId(conversation, userId))
                .collect(Collectors.toList());
        Map<Long, WfUser> userMap = userService.getUserMap(targetUserIds);

        List<ConversationVO> result = new ArrayList<>(conversations.size());
        for (WfConversation conversation : conversations) {
            Long targetUserId = getTargetUserId(conversation, userId);
            WfUser targetUser = userMap.get(targetUserId);
            if (targetUser == null) {
                continue;
            }

            ConversationVO vo = new ConversationVO();
            vo.setConversationId(conversation.getId());
            vo.setTargetUserId(targetUserId);
            vo.setTargetWolfNo(targetUser.getWolfNo());
            vo.setTargetNickname(targetUser.getNickname());
            vo.setTargetEquippedTitleName(targetUser.getEquippedTitleName());
            vo.setTargetEquippedTitleColor(targetUser.getEquippedTitleColor());
            vo.setTargetAvatar(targetUser.getAvatar());
            vo.setLastMessage(conversation.getLastMessage());
            vo.setLastMessageTime(conversation.getLastMessageTime());
            vo.setUnreadCount(getUnreadCount(conversation, userId));
            result.add(vo);
        }
        userPresenceService.fillConversationPresence(result);
        return result;
    }

    public List<Long> listPeerUserIds(Long userId) {
        LambdaQueryWrapper<WfConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.eq(WfConversation::getUserAId, userId)
                        .or()
                        .eq(WfConversation::getUserBId, userId))
                .select(WfConversation::getUserAId, WfConversation::getUserBId);
        List<WfConversation> conversations = conversationMapper.selectList(queryWrapper);
        if (conversations.isEmpty()) {
            return Collections.emptyList();
        }
        return conversations.stream()
                .map(conversation -> getTargetUserId(conversation, userId))
                .distinct()
                .collect(Collectors.toList());
    }

    public long getUnreadTotal(Long userId) {
        Long total = conversationMapper.selectUnreadTotalByUserId(userId);
        if (total == null || total < 0) {
            return 0L;
        }
        return total;
    }

    private Integer getUnreadCount(WfConversation conversation, Long userId) {
        return isUserA(conversation, userId)
                ? normalizeCount(conversation.getUserAUnreadCount())
                : normalizeCount(conversation.getUserBUnreadCount());
    }

    private boolean isUserA(WfConversation conversation, Long userId) {
        if (conversation.getUserAId().equals(userId)) {
            return true;
        }
        if (conversation.getUserBId().equals(userId)) {
            return false;
        }
        throw new BaseException(ErrorCode.NOT_CONVERSATION_MEMBER);
    }

    private int normalizeCount(Integer count) {
        return count == null || count < 0 ? 0 : count;
    }

}
