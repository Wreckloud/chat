package com.wreckloud.wolfchat.chat.websocket.handler.type;

import com.wreckloud.wolfchat.chat.lobby.api.vo.LobbyMessageVO;
import com.wreckloud.wolfchat.chat.lobby.application.service.LobbyService;
import com.wreckloud.wolfchat.chat.message.application.service.ChatMessagePushService;
import com.wreckloud.wolfchat.chat.websocket.dto.WsRequest;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.chat.websocket.handler.WsTypeRequestHandler;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsAuthenticatedExecutor;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsResponseSupport;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * @Description LOBBY_RECALL 类型 WS 请求处理
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class WsLobbyRecallRequestHandler implements WsTypeRequestHandler {
    private final WsAuthenticatedExecutor wsAuthenticatedExecutor;
    private final LobbyService lobbyService;
    private final ChatMessagePushService chatMessagePushService;
    private final WsResponseSupport wsResponseSupport;

    @Override
    public WsType type() {
        return WsType.LOBBY_RECALL;
    }

    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        wsAuthenticatedExecutor.execute(session, request, "撤回大厅消息", (userId, clientMsgId, currentRequest) -> {
            if (currentRequest.getMessageId() == null || currentRequest.getMessageId() <= 0L) {
                wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "消息ID不能为空", clientMsgId);
                return;
            }
            LobbyMessageVO messageVO = lobbyService.recallMessage(userId, currentRequest.getMessageId());
            wsResponseSupport.sendAck(session, clientMsgId, messageVO);
            chatMessagePushService.pushLobbyMessage(userId, messageVO);
        });
    }
}

