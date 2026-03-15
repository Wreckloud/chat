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
import com.wreckloud.wolfchat.chat.message.application.support.MessageRuleSupport;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import com.wreckloud.wolfchat.chat.presence.application.service.UserPresenceService;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.storage.service.OssStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final String VIDEO_POSTER_PROCESS = "video/snapshot,t_1000,f_jpg,w_480,m_fast";

    private final WfLobbyMessageMapper lobbyMessageMapper;
    private final UserService userService;
    private final ChatMediaService chatMediaService;
    private final UserPresenceService userPresenceService;
    private final OssStorageService ossStorageService;

    /**
     * 发送大厅消息
     */
    @Transactional(rollbackFor = Exception.class)
    public LobbyMessageVO sendMessage(SendLobbyMessageCommand command) {
        Long userId = command.getUserId();
        MessageType msgType = MessageRuleSupport.normalizeMessageType(command.getMsgType());
        String normalizedContent = MessageRuleSupport.normalizeContent(command.getContent(), msgType);
        command.setMsgType(msgType);

        userService.getEnabledByIdOrThrow(userId);

        chatMediaService.validateMessagePayload(userId, toChatMediaCommand(command));

        WfLobbyMessage message = new WfLobbyMessage();
        message.setSenderId(userId);
        message.setContent(normalizedContent);
        message.setMsgType(msgType);
        message.setMediaKey(command.getMediaKey());
        message.setMediaWidth(command.getMediaWidth());
        message.setMediaHeight(command.getMediaHeight());
        message.setMediaSize(command.getMediaSize());
        message.setMediaMimeType(command.getMediaMimeType());
        message.setCreateTime(LocalDateTime.now());
        int insertRows = lobbyMessageMapper.insert(message);
        if (insertRows != 1) {
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
            item.setAvatar(user.getAvatar());
            item.setOnline(snapshot.isOnline());
            item.setLastActiveAt(snapshot.getLastActiveAt());
            recentUsers.add(item);
        }

        LobbyMetaVO meta = new LobbyMetaVO();
        meta.setOnlineCount(userPresenceService.countOnlineUsers());
        meta.setLatestActiveAt(recentUsers.isEmpty() ? null : recentUsers.get(0).getLastActiveAt());
        meta.setRecentUsers(recentUsers);
        return meta;
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
            vo.setMediaPosterUrl(ossStorageService.buildSignedReadUrl(vo.getMediaKey(), VIDEO_POSTER_PROCESS));
        }
        return vo;
    }

    private SendMessageCommand toChatMediaCommand(SendLobbyMessageCommand command) {
        SendMessageCommand mediaCommand = new SendMessageCommand();
        mediaCommand.setUserId(command.getUserId());
        mediaCommand.setContent(command.getContent());
        mediaCommand.setMsgType(command.getMsgType());
        mediaCommand.setMediaKey(command.getMediaKey());
        mediaCommand.setMediaWidth(command.getMediaWidth());
        mediaCommand.setMediaHeight(command.getMediaHeight());
        mediaCommand.setMediaSize(command.getMediaSize());
        mediaCommand.setMediaMimeType(command.getMediaMimeType());
        return mediaCommand;
    }

}
