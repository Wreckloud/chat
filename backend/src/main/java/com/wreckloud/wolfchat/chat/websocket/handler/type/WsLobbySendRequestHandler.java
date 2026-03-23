package com.wreckloud.wolfchat.chat.websocket.handler.type;

import com.wreckloud.wolfchat.chat.lobby.api.vo.LobbyMessageVO;
import com.wreckloud.wolfchat.chat.lobby.application.command.SendLobbyMessageCommand;
import com.wreckloud.wolfchat.chat.lobby.application.service.LobbyService;
import com.wreckloud.wolfchat.chat.message.application.service.ChatMessagePushService;
import com.wreckloud.wolfchat.chat.websocket.dto.WsRequest;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.chat.websocket.handler.WsTypeRequestHandler;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsAuthenticatedExecutor;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsResponseSupport;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsSendDedupStore;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * @Description LOBBY_SEND 类型 WS 请求处理
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class WsLobbySendRequestHandler implements WsTypeRequestHandler {
    private final WsAuthenticatedExecutor wsAuthenticatedExecutor;
    private final LobbyService lobbyService;
    private final ChatMessagePushService chatMessagePushService;
    private final WsResponseSupport wsResponseSupport;
    private final WsSendDedupStore wsSendDedupStore;

    @Override
    public WsType type() {
        return WsType.LOBBY_SEND;
    }

    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        wsAuthenticatedExecutor.execute(session, request, "发送大厅消息", (userId, clientMsgId, currentRequest) -> {
            String dedupKey = wsSendDedupStore.buildKey(WsType.LOBBY_SEND, userId, clientMsgId);
            LobbyMessageVO duplicateMessage = resolveDuplicateMessage(session, clientMsgId, dedupKey);
            if (duplicateMessage != null) {
                return;
            }
            if (!wsSendDedupStore.markPending(dedupKey)) {
                wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "请求处理中，请勿重复发送", clientMsgId);
                return;
            }
            try {
                LobbyMessageVO messageVO = lobbyService.sendMessage(buildCommand(userId, currentRequest));
                wsResponseSupport.sendAck(session, clientMsgId, messageVO);
                chatMessagePushService.pushLobbyMessage(userId, messageVO);
                wsSendDedupStore.storeMessageId(dedupKey, messageVO.getMessageId());
            } catch (Exception e) {
                wsSendDedupStore.clearPending(dedupKey);
                throw e;
            }
        });
    }

    private LobbyMessageVO resolveDuplicateMessage(WebSocketSession session, String clientMsgId, String dedupKey) {
        Long duplicateMessageId = wsSendDedupStore.getMessageId(dedupKey);
        if (duplicateMessageId == null) {
            return null;
        }
        LobbyMessageVO messageVO = lobbyService.getMessageById(duplicateMessageId);
        if (messageVO == null) {
            wsSendDedupStore.delete(dedupKey);
            return null;
        }
        wsResponseSupport.sendAck(session, clientMsgId, messageVO);
        return messageVO;
    }

    private SendLobbyMessageCommand buildCommand(Long userId, WsRequest request) {
        SendLobbyMessageCommand command = new SendLobbyMessageCommand();
        command.setUserId(userId);
        command.setClientMsgId(request.getClientMsgId());
        command.setContent(request.getContent());
        command.setMsgType(request.getMsgType());
        command.setMediaKey(request.getMediaKey());
        command.setMediaPosterKey(request.getMediaPosterKey());
        command.setMediaWidth(request.getMediaWidth());
        command.setMediaHeight(request.getMediaHeight());
        command.setMediaSize(request.getMediaSize());
        command.setMediaMimeType(request.getMediaMimeType());
        command.setReplyToMessageId(request.getReplyToMessageId());
        return command;
    }
}

