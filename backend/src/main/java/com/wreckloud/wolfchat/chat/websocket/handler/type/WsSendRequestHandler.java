package com.wreckloud.wolfchat.chat.websocket.handler.type;

import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.message.application.command.SendMessageCommand;
import com.wreckloud.wolfchat.chat.message.application.service.ChatMessagePushService;
import com.wreckloud.wolfchat.chat.message.application.service.MessageService;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageDeliveryStatus;
import com.wreckloud.wolfchat.chat.websocket.dto.WsRequest;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.chat.websocket.handler.WsTypeRequestHandler;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsAuthenticatedExecutor;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsMessageBuildSupport;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsResponseSupport;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsSendDedupStore;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * @Description SEND 类型 WS 请求处理
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class WsSendRequestHandler implements WsTypeRequestHandler {
    private final WsAuthenticatedExecutor wsAuthenticatedExecutor;
    private final MessageService messageService;
    private final ChatMessagePushService chatMessagePushService;
    private final WsMessageBuildSupport wsMessageBuildSupport;
    private final WsResponseSupport wsResponseSupport;
    private final WsSendDedupStore wsSendDedupStore;

    @Override
    public WsType type() {
        return WsType.SEND;
    }

    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        wsAuthenticatedExecutor.execute(session, request, "发送消息", (userId, clientMsgId, currentRequest) -> {
            if (currentRequest.getConversationId() == null) {
                wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "会话ID不能为空", clientMsgId);
                return;
            }
            String dedupKey = wsSendDedupStore.buildKey(WsType.SEND, userId, clientMsgId);
            MessageVO duplicateMessage = resolveDuplicateMessageVO(session, clientMsgId, dedupKey);
            if (duplicateMessage != null) {
                return;
            }
            if (!wsSendDedupStore.markPending(dedupKey)) {
                wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "请求处理中，请勿重复发送", clientMsgId);
                return;
            }
            try {
                WfMessage message = messageService.sendMessage(buildSendCommand(userId, currentRequest));
                MessageVO messageVO = wsMessageBuildSupport.buildMessageVOWithSender(message);
                wsResponseSupport.sendAck(session, clientMsgId, messageVO);
                chatMessagePushService.pushPrivateMessageToReceiver(message);
                wsSendDedupStore.storeMessageId(dedupKey, message.getId());
            } catch (Exception e) {
                wsSendDedupStore.clearPending(dedupKey);
                throw e;
            }
        });
    }

    private MessageVO resolveDuplicateMessageVO(WebSocketSession session, String clientMsgId, String dedupKey) {
        Long duplicateMessageId = wsSendDedupStore.getMessageId(dedupKey);
        if (duplicateMessageId == null) {
            return null;
        }
        WfMessage duplicateMessage = messageService.getById(duplicateMessageId);
        if (duplicateMessage == null) {
            wsSendDedupStore.delete(dedupKey);
            return null;
        }

        MessageVO messageVO = wsMessageBuildSupport.buildMessageVOWithSender(duplicateMessage);
        wsResponseSupport.sendAck(session, clientMsgId, messageVO);
        if (MessageDeliveryStatus.UNDELIVERED.equals(duplicateMessage.getDelivered())) {
            chatMessagePushService.pushPrivateMessageToReceiver(duplicateMessage);
        }
        return messageVO;
    }

    private SendMessageCommand buildSendCommand(Long userId, WsRequest request) {
        SendMessageCommand command = new SendMessageCommand();
        command.setUserId(userId);
        command.setConversationId(request.getConversationId());
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

