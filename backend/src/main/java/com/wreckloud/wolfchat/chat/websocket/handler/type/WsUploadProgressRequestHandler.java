package com.wreckloud.wolfchat.chat.websocket.handler.type;

import com.alibaba.fastjson.JSON;
import com.wreckloud.wolfchat.chat.conversation.application.service.ConversationService;
import com.wreckloud.wolfchat.chat.conversation.domain.entity.WfConversation;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import com.wreckloud.wolfchat.chat.websocket.dto.UploadProgressPayload;
import com.wreckloud.wolfchat.chat.websocket.dto.WsRequest;
import com.wreckloud.wolfchat.chat.websocket.dto.WsResponse;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.chat.websocket.handler.WsTypeRequestHandler;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsAuthenticatedExecutor;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsMessageBuildSupport;
import com.wreckloud.wolfchat.chat.websocket.handler.support.WsResponseSupport;
import com.wreckloud.wolfchat.chat.websocket.session.WsSessionManager;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

/**
 * @Description UPLOAD_PROGRESS 类型 WS 请求处理
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class WsUploadProgressRequestHandler implements WsTypeRequestHandler {
    private static final String SEND_TYPE_PRIVATE = "SEND";
    private static final String SEND_TYPE_LOBBY = "LOBBY_SEND";
    private static final Set<String> ALLOWED_UPLOAD_STATUS = Set.of("UPLOADING", "SENDING", "FAILED");

    private final WsAuthenticatedExecutor wsAuthenticatedExecutor;
    private final WsResponseSupport wsResponseSupport;
    private final WsMessageBuildSupport wsMessageBuildSupport;
    private final WsSessionManager sessionManager;
    private final ConversationService conversationService;

    @Override
    public WsType type() {
        return WsType.UPLOAD_PROGRESS;
    }

    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        wsAuthenticatedExecutor.execute(session, request, "同步上传进度", (userId, clientMsgId, currentRequest) -> {
            String sendType = validateAndResolveSendType(session, currentRequest, clientMsgId, userId);
            if (sendType == null) {
                return;
            }
            UploadProgressPayload payload = wsMessageBuildSupport.buildUploadProgressPayload(userId, currentRequest, sendType);
            WsResponse response = wsResponseSupport.buildResponse(WsType.UPLOAD_PROGRESS, payload);
            String encodedPayload = JSON.toJSONString(response);

            if (SEND_TYPE_LOBBY.equals(sendType)) {
                sessionManager.sendToAll(encodedPayload, userId);
                return;
            }

            WfConversation conversation = conversationService.getConversation(currentRequest.getConversationId());
            Long peerUserId = conversationService.getTargetUserId(conversation, userId);
            sessionManager.sendToUser(peerUserId, encodedPayload);
        });
    }

    private String validateAndResolveSendType(WebSocketSession session, WsRequest request, String clientMsgId, Long userId) {
        String sendType = request.getSendType() == null ? "" : request.getSendType().trim().toUpperCase();
        if (!SEND_TYPE_PRIVATE.equals(sendType) && !SEND_TYPE_LOBBY.equals(sendType)) {
            wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "发送场景不正确", clientMsgId);
            return null;
        }
        if (!StringUtils.hasText(request.getClientMsgId())) {
            wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "客户端消息ID不能为空", clientMsgId);
            return null;
        }
        MessageType msgType = request.getMsgType();
        if (!MessageType.IMAGE.equals(msgType)
                && !MessageType.VIDEO.equals(msgType)
                && !MessageType.FILE.equals(msgType)) {
            wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "消息类型不支持进度同步", clientMsgId);
            return null;
        }
        int progress = request.getUploadProgress() == null ? -1 : request.getUploadProgress();
        if (progress < 0 || progress > 100) {
            wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "上传进度不合法", clientMsgId);
            return null;
        }
        String uploadStatus = request.getUploadStatus() == null ? "" : request.getUploadStatus().trim().toUpperCase();
        if (!ALLOWED_UPLOAD_STATUS.contains(uploadStatus)) {
            wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "上传状态不合法", clientMsgId);
            return null;
        }

        if (SEND_TYPE_PRIVATE.equals(sendType)) {
            if (request.getConversationId() == null || request.getConversationId() <= 0L) {
                wsResponseSupport.sendError(session, ErrorCode.PARAM_ERROR, "会话ID不能为空", clientMsgId);
                return null;
            }
            conversationService.validateConversationMember(request.getConversationId(), userId);
        }
        return sendType;
    }
}

