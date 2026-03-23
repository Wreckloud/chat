package com.wreckloud.wolfchat.chat.websocket.handler.type;

import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.message.application.service.ChatMessagePushService;
import com.wreckloud.wolfchat.chat.message.application.service.MessageService;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.chat.websocket.dto.WsRequest;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.chat.websocket.handler.WsTypeRequestHandler;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsAuthenticatedExecutor;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsMessageBuildSupport;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsResponseSupport;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * @Description RECALL 类型 WS 请求处理
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class WsRecallRequestHandler implements WsTypeRequestHandler {
    private final WsAuthenticatedExecutor wsAuthenticatedExecutor;
    private final MessageService messageService;
    private final ChatMessagePushService chatMessagePushService;
    private final WsMessageBuildSupport wsMessageBuildSupport;
    private final WsResponseSupport wsResponseSupport;

    @Override
    public WsType type() {
        return WsType.RECALL;
    }

    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        wsAuthenticatedExecutor.execute(session, request, "撤回消息", (userId, clientMsgId, currentRequest) -> {
            if (currentRequest.getConversationId() == null) {
                wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "会话ID不能为空", clientMsgId);
                return;
            }
            if (currentRequest.getMessageId() == null || currentRequest.getMessageId() <= 0L) {
                wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "消息ID不能为空", clientMsgId);
                return;
            }
            WfMessage message = messageService.recallMessage(
                    userId,
                    currentRequest.getConversationId(),
                    currentRequest.getMessageId()
            );
            MessageVO messageVO = wsMessageBuildSupport.buildMessageVOWithSender(message);
            wsResponseSupport.sendAck(session, clientMsgId, messageVO);
            chatMessagePushService.pushPrivateMessageToConversationPeers(messageVO, message.getSenderId(), message.getReceiverId());
        });
    }
}

