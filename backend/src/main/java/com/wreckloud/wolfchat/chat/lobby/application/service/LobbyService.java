package com.wreckloud.wolfchat.chat.lobby.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.chat.lobby.api.converter.LobbyMessageConverter;
import com.wreckloud.wolfchat.chat.lobby.api.vo.LobbyMessageVO;
import com.wreckloud.wolfchat.chat.lobby.api.vo.LobbyMetaVO;
import com.wreckloud.wolfchat.chat.lobby.api.vo.LobbyRecentUserVO;
import com.wreckloud.wolfchat.chat.lobby.application.command.SendLobbyMessageCommand;
import com.wreckloud.wolfchat.chat.lobby.domain.entity.WfLobbyMessage;
import com.wreckloud.wolfchat.chat.lobby.infra.mapper.WfLobbyMessageMapper;
import com.wreckloud.wolfchat.chat.media.application.service.ChatMediaService;
import com.wreckloud.wolfchat.chat.message.application.command.SendMessageCommand;
import com.wreckloud.wolfchat.chat.message.application.service.MessageMediaService;
import com.wreckloud.wolfchat.chat.message.application.support.MessageRuleSupport;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import com.wreckloud.wolfchat.chat.presence.application.service.UserPresenceService;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.storage.service.OssStorageService;
import com.wreckloud.wolfchat.notice.application.service.UserNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description 大厅服务
 * @Author Wreckloud
 * @Date 2026-03-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int RECENT_USER_LIMIT = 12;
    private static final int RECALL_WINDOW_MINUTES = 5;
    private static final int CLIENT_MSG_ID_MAX_LENGTH = 64;
    private static final String RECALL_CONTENT = "该消息已撤回";
    private static final String VIDEO_POSTER_PROCESS = "video/snapshot,t_1000,f_jpg,w_480,m_fast";

    private final WfLobbyMessageMapper lobbyMessageMapper;
    private final UserService userService;
    private final ChatMediaService chatMediaService;
    private final MessageMediaService messageMediaService;
    private final UserPresenceService userPresenceService;
    private final OssStorageService ossStorageService;
    private final UserNoticeService userNoticeService;

    /**
     * 发送大厅消息
     */
    @Transactional(rollbackFor = Exception.class)
    public LobbyMessageVO sendMessage(SendLobbyMessageCommand command) {
        Long userId = command.getUserId();
        String clientMsgId = normalizeClientMsgId(command.getClientMsgId());
        command.setClientMsgId(clientMsgId);

        WfLobbyMessage existingMessage = findExistingMessageByClientMsgId(userId, clientMsgId);
        if (existingMessage != null) {
            WfUser existingSender = userService.getByIdOrThrow(existingMessage.getSenderId());
            return fillMedia(LobbyMessageConverter.toLobbyMessageVO(existingMessage, existingSender));
        }

        MessageType msgType = MessageRuleSupport.normalizeMessageType(command.getMsgType());
        String normalizedContent = MessageRuleSupport.normalizeContent(command.getContent(), msgType);
        command.setMsgType(msgType);

        userService.getEnabledByIdOrThrow(userId);

        chatMediaService.validateMessagePayload(userId, toChatMediaCommand(command));
        WfLobbyMessage replyToMessage = resolveReplyTargetMessage(command.getReplyToMessageId());

        WfLobbyMessage message = new WfLobbyMessage();
        message.setSenderId(userId);
        message.setClientMsgId(clientMsgId);
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
            message.setReplyToPreview(
                    messageMediaService.buildReplyPreview(replyToMessage.getMsgType(), replyToMessage.getContent())
            );
        }
        message.setCreateTime(LocalDateTime.now());
        try {
            int insertRows = lobbyMessageMapper.insert(message);
            if (insertRows != 1) {
                throw new BaseException(ErrorCode.DATABASE_ERROR);
            }
        } catch (DuplicateKeyException ex) {
            WfLobbyMessage duplicatedMessage = findExistingMessageByClientMsgId(userId, clientMsgId);
            if (duplicatedMessage != null) {
                WfUser existingSender = userService.getByIdOrThrow(duplicatedMessage.getSenderId());
                return fillMedia(LobbyMessageConverter.toLobbyMessageVO(duplicatedMessage, existingSender));
            }
            throw ex;
        }
        if (replyToMessage != null) {
            userNoticeService.notifyLobbyMessageReplied(replyToMessage.getSenderId(), userId);
        }

        WfUser sender = userService.getByIdOrThrow(userId);
        return fillMedia(LobbyMessageConverter.toLobbyMessageVO(message, sender));
    }

    /**
     * 撤回大厅消息（仅发送者在 5 分钟内可撤回）
     */
    @Transactional(rollbackFor = Exception.class)
    public LobbyMessageVO recallMessage(Long userId, Long messageId) {
        if (messageId == null || messageId <= 0L) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "消息ID不能为空");
        }
        userService.getEnabledByIdOrThrow(userId);

        WfLobbyMessage message = lobbyMessageMapper.selectById(messageId);
        if (message == null) {
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
        int updateRows = lobbyMessageMapper.updateById(message);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
        WfUser sender = userService.getByIdOrThrow(userId);
        return fillMedia(LobbyMessageConverter.toLobbyMessageVO(message, sender));
    }

    /**
     * 分页查询大厅消息
     */
    public Page<LobbyMessageVO> listMessages(Long userId, Integer pageNum, Integer pageSize) {
        userService.getEnabledByIdOrThrow(userId);
        MessageRuleSupport.validatePageParams(pageNum, pageSize, MAX_PAGE_SIZE);

        Page<WfLobbyMessage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WfLobbyMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(WfLobbyMessage::getCreateTime)
                .orderByDesc(WfLobbyMessage::getId);
        Page<WfLobbyMessage> messagePage = lobbyMessageMapper.selectPage(page, queryWrapper);

        List<Long> senderIds = messagePage.getRecords().stream()
                .map(WfLobbyMessage::getSenderId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, WfUser> senderMap = userService.getUserMap(senderIds);

        Page<LobbyMessageVO> result = LobbyMessageConverter.toLobbyMessageVOPage(messagePage, senderMap);
        for (LobbyMessageVO item : result.getRecords()) {
            fillMedia(item);
        }
        return result;
    }

    public LobbyMessageVO getMessageById(Long messageId) {
        if (messageId == null || messageId <= 0L) {
            return null;
        }
        WfLobbyMessage message = lobbyMessageMapper.selectById(messageId);
        if (message == null) {
            return null;
        }
        WfUser sender = userService.getByIdOrThrow(message.getSenderId());
        return fillMedia(LobbyMessageConverter.toLobbyMessageVO(message, sender));
    }

    /**
     * 查询大厅元信息
     */
    public LobbyMetaVO getMeta(Long userId) {
        userService.getEnabledByIdOrThrow(userId);

        List<UserPresenceService.PresenceSnapshot> snapshots =
                userPresenceService.listRecentActiveUsers(RECENT_USER_LIMIT);
        List<Long> userIds = snapshots.stream()
                .map(UserPresenceService.PresenceSnapshot::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, WfUser> userMap = userService.getUserMap(userIds);

        List<LobbyRecentUserVO> recentUsers = new ArrayList<>();
        for (UserPresenceService.PresenceSnapshot snapshot : snapshots) {
            WfUser user = userMap.get(snapshot.getUserId());
            if (user == null) {
                continue;
            }
            LobbyRecentUserVO item = new LobbyRecentUserVO();
            item.setUserId(user.getId());
            item.setWolfNo(user.getWolfNo());
            item.setNickname(user.getNickname());
            item.setEquippedTitleName(user.getEquippedTitleName());
            item.setEquippedTitleColor(user.getEquippedTitleColor());
            item.setAvatar(user.getAvatar());
            item.setOnline(snapshot.isOnline());
            item.setLastActiveAt(snapshot.getLastActiveAt());
            recentUsers.add(item);
        }

        LobbyMetaVO meta = new LobbyMetaVO();
        meta.setOnlineCount(userPresenceService.countOnlineUsers());
        meta.setLatestActiveAt(recentUsers.isEmpty() ? null : recentUsers.get(0).getLastActiveAt());
        meta.setRecentUsers(recentUsers);
        fillLatestMessageMeta(meta);
        return meta;
    }

    private void fillLatestMessageMeta(LobbyMetaVO meta) {
        LambdaQueryWrapper<WfLobbyMessage> latestMessageQuery = new LambdaQueryWrapper<>();
        latestMessageQuery.orderByDesc(WfLobbyMessage::getCreateTime)
                .orderByDesc(WfLobbyMessage::getId)
                .last("LIMIT 1");
        WfLobbyMessage latestMessage = lobbyMessageMapper.selectOne(latestMessageQuery);
        if (latestMessage == null) {
            meta.setLatestMessagePreview("");
            meta.setLatestMessageSenderName("");
            meta.setLatestMessageAt(null);
            return;
        }
        meta.setLatestMessageSenderName(resolveLobbyMessageSenderName(latestMessage.getSenderId()));
        meta.setLatestMessagePreview(
                messageMediaService.buildConversationPreview(latestMessage.getMsgType(), latestMessage.getContent())
        );
        meta.setLatestMessageAt(latestMessage.getCreateTime());
    }

    private String resolveLobbyMessageSenderName(Long senderId) {
        if (senderId == null || senderId <= 0L) {
            return "";
        }
        Map<Long, WfUser> userMap = userService.getUserMap(List.of(senderId));
        WfUser sender = userMap.get(senderId);
        if (sender == null) {
            return "";
        }
        if (StringUtils.hasText(sender.getNickname())) {
            return sender.getNickname().trim();
        }
        if (StringUtils.hasText(sender.getWolfNo())) {
            return sender.getWolfNo().trim();
        }
        return "";
    }

    private LobbyMessageVO fillMedia(LobbyMessageVO vo) {
        if (vo == null || vo.getMediaKey() == null || vo.getMediaKey().trim().isEmpty()) {
            return vo;
        }
        MessageType msgType = vo.getMsgType();
        if (!MessageType.IMAGE.equals(msgType)
                && !MessageType.VIDEO.equals(msgType)
                && !MessageType.FILE.equals(msgType)) {
            return vo;
        }
        vo.setMediaUrl(ossStorageService.buildSignedReadUrl(vo.getMediaKey()));
        if (MessageType.VIDEO.equals(msgType)) {
            if (vo.getMediaPosterKey() != null && !vo.getMediaPosterKey().trim().isEmpty()) {
                vo.setMediaPosterUrl(ossStorageService.buildSignedReadUrl(vo.getMediaPosterKey()));
            } else {
                vo.setMediaPosterUrl(ossStorageService.buildSignedReadUrl(vo.getMediaKey(), VIDEO_POSTER_PROCESS));
            }
        }
        return vo;
    }

    private WfLobbyMessage resolveReplyTargetMessage(Long replyToMessageId) {
        if (replyToMessageId == null || replyToMessageId <= 0L) {
            return null;
        }
        WfLobbyMessage replyToMessage = lobbyMessageMapper.selectById(replyToMessageId);
        if (replyToMessage == null) {
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

    private WfLobbyMessage findExistingMessageByClientMsgId(Long senderId, String clientMsgId) {
        if (senderId == null || senderId <= 0L || !StringUtils.hasText(clientMsgId)) {
            return null;
        }
        return lobbyMessageMapper.selectBySenderAndClientMsgId(senderId, clientMsgId);
    }

    private SendMessageCommand toChatMediaCommand(SendLobbyMessageCommand command) {
        SendMessageCommand mediaCommand = new SendMessageCommand();
        mediaCommand.setUserId(command.getUserId());
        mediaCommand.setClientMsgId(command.getClientMsgId());
        mediaCommand.setContent(command.getContent());
        mediaCommand.setMsgType(command.getMsgType());
        mediaCommand.setMediaKey(command.getMediaKey());
        mediaCommand.setMediaPosterKey(command.getMediaPosterKey());
        mediaCommand.setMediaWidth(command.getMediaWidth());
        mediaCommand.setMediaHeight(command.getMediaHeight());
        mediaCommand.setMediaSize(command.getMediaSize());
        mediaCommand.setMediaMimeType(command.getMediaMimeType());
        mediaCommand.setReplyToMessageId(command.getReplyToMessageId());
        return mediaCommand;
    }

}
