package com.wreckloud.wolfchat.chat.message.application.service;

import com.alibaba.fastjson.JSON;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.chat.lobby.api.vo.LobbyMessageVO;
import com.wreckloud.wolfchat.chat.message.api.converter.MessageConverter;
import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageDeliveryStatus;
import com.wreckloud.wolfchat.chat.message.infra.mapper.WfMessageMapper;
import com.wreckloud.wolfchat.chat.websocket.dto.WsResponse;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.chat.websocket.session.WsSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 聊天消息实时推送服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessagePushService {
    private final WsSessionManager wsSessionManager;
    private final WfMessageMapper wfMessageMapper;
    private final UserService userService;
    private final MessageMediaService messageMediaService;

    /**
     * 推送私聊消息给接收者，在线即标记送达。
     */
    public void pushPrivateMessageToReceiver(WfMessage message) {
        if (message == null || message.getReceiverId() == null || message.getReceiverId() <= 0L) {
            return;
        }
        if (!MessageDeliveryStatus.UNDELIVERED.equals(message.getDelivered())) {
            return;
        }
        MessageVO messageVO = buildMessageVO(message);
        WsResponse response = new WsResponse();
        response.setType(WsType.MESSAGE);
        response.setData(messageVO);
        int successCount = wsSessionManager.sendToUser(message.getReceiverId(), JSON.toJSONString(response));
        if (successCount <= 0) {
            return;
        }
        WfMessage delivered = new WfMessage();
        delivered.setId(message.getId());
        delivered.setDelivered(MessageDeliveryStatus.DELIVERED);
        delivered.setDeliveredTime(LocalDateTime.now());
        wfMessageMapper.updateById(delivered);
    }

    /**
     * 推送消息给会话双方（用于撤回等同步场景）。
     */
    public void pushPrivateMessageToConversationPeers(MessageVO messageVO, Long senderUserId, Long receiverUserId) {
        if (messageVO == null || senderUserId == null || receiverUserId == null) {
            return;
        }
        WsResponse response = new WsResponse();
        response.setType(WsType.MESSAGE);
        response.setData(messageVO);
        String payload = JSON.toJSONString(response);
        wsSessionManager.sendToUser(senderUserId, payload);
        wsSessionManager.sendToUser(receiverUserId, payload);
    }

    /**
     * 推送公共聊天室消息给所有在线用户（排除发送者自身）。
     */
    public void pushLobbyMessage(Long senderUserId, LobbyMessageVO messageVO) {
        if (messageVO == null) {
            return;
        }
        WsResponse response = new WsResponse();
        response.setType(WsType.LOBBY_MESSAGE);
        response.setData(messageVO);
        wsSessionManager.sendToAll(JSON.toJSONString(response), senderUserId);
    }

    /**
     * 推送大厅消息给所有在线用户（不排除发送者）。
     */
    public void pushLobbyMessageToAll(LobbyMessageVO messageVO) {
        if (messageVO == null) {
            return;
        }
        WsResponse response = new WsResponse();
        response.setType(WsType.LOBBY_MESSAGE);
        response.setData(messageVO);
        wsSessionManager.sendToAll(JSON.toJSONString(response), null);
    }

    private MessageVO buildMessageVO(WfMessage message) {
        WfUser sender = null;
        if (message.getSenderId() != null && message.getSenderId() > 0L) {
            sender = userService.getByIdOrThrow(message.getSenderId());
        }
        MessageVO messageVO = MessageConverter.toMessageVO(message, sender);
        return messageMediaService.fillMedia(messageVO);
    }
}
