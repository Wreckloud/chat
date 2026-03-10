package com.wreckloud.wolfchat.chat.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.chat.message.api.converter.MessageConverter;
import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.message.application.command.SendMessageCommand;
import com.wreckloud.wolfchat.chat.message.application.service.MessageMediaService;
import com.wreckloud.wolfchat.chat.message.application.service.MessageService;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.chat.websocket.dto.WsRequest;
import com.wreckloud.wolfchat.chat.websocket.dto.WsResponse;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.chat.websocket.session.WsSessionManager;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.service.SessionUserService;
import com.wreckloud.wolfchat.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description WebSocket 消息处理
 * @Author Wreckloud
 * @Date 2026-02-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final JwtUtil jwtUtil;
    private final MessageService messageService;
    private final MessageMediaService messageMediaService;
    private final UserService userService;
    private final WsSessionManager sessionManager;
    private final SessionUserService sessionUserService;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        log.info("WS 连接建立: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        WsRequest request = parseRequest(session, message.getPayload());
        if (request == null) {
            return;
        }

        switch (request.getType()) {
            case AUTH:
                handleAuth(session, request);
                break;
            case SEND:
                handleSend(session, request);
                break;
            case PING:
                handlePing(session);
                break;
            default:
                sendError(session, ErrorCode.PARAM_ERROR, "不支持的消息类型");
                break;
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessionManager.removeSession(session);
        log.info("WS 连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    private WsRequest parseRequest(WebSocketSession session, String payload) {
        WsRequest request;
        try {
            request = JSON.parseObject(payload, WsRequest.class);
        } catch (Exception e) {
            sendError(session, ErrorCode.PARAM_ERROR, "消息格式错误");
            return null;
        }

        if (request == null || request.getType() == null) {
            sendError(session, ErrorCode.PARAM_ERROR, "消息类型不能为空");
            return null;
        }
        return request;
    }

    private void handleAuth(WebSocketSession session, WsRequest request) {
        Long userId = authenticateUserId(session, request.getToken());
        if (userId == null) {
            return;
        }

        sessionManager.addSession(userId, session);
        log.info("WS 认证成功: userId={}, sessionId={}", userId, session.getId());
        sendAuthOk(session);
        replayUndeliveredMessages(session, userId);
    }

    private void handlePing(WebSocketSession session) {
        Long userId = sessionManager.getUserId(session);
        if (userId == null) {
            sendError(session, ErrorCode.UNAUTHORIZED, "请先认证");
            return;
        }
        sessionManager.refreshOnline(userId);
    }

    private Long authenticateUserId(WebSocketSession session, String rawToken) {
        String token = normalizeToken(rawToken);
        if (!StringUtils.hasText(token)) {
            sendError(session, ErrorCode.UNAUTHORIZED, "token 不能为空");
            return null;
        }
        if (!jwtUtil.validateToken(token)) {
            sendError(session, ErrorCode.TOKEN_INVALID, "token 无效");
            return null;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            sendError(session, ErrorCode.TOKEN_INVALID, "token 无效");
            return null;
        }
        if (!sessionUserService.isSessionUserExists(userId)) {
            sendError(session, ErrorCode.TOKEN_INVALID, "token 无效");
            return null;
        }
        return userId;
    }

    private String normalizeToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return null;
        }
        String token = rawToken.trim();
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        return token;
    }

    private void sendAuthOk(WebSocketSession session) {
        send(session, buildResponse(WsType.AUTH_OK, null));
    }

    private void replayUndeliveredMessages(WebSocketSession session, Long userId) {
        // 离线补发只对当前认证会话执行；仅当推送成功后再标记 delivered。
        List<WfMessage> undelivered = messageService.listUndeliveredMessages(userId);
        if (undelivered.isEmpty()) {
            return;
        }

        log.info("WS 补发未送达消息: userId={}, count={}", userId, undelivered.size());
        List<Long> deliveredIds = new ArrayList<>();
        for (WfMessage message : undelivered) {
            WsResponse push = buildMessageResponse(buildMessageVOWithSender(message));
            if (send(session, push)) {
                deliveredIds.add(message.getId());
            }
        }
        if (!deliveredIds.isEmpty()) {
            messageService.markDelivered(deliveredIds);
        }
    }

    private void handleSend(WebSocketSession session, WsRequest request) {
        String clientMsgId = request.getClientMsgId();
        Long userId = sessionManager.getUserId(session);
        if (userId == null) {
            sendError(session, ErrorCode.UNAUTHORIZED, "请先认证", clientMsgId);
            return;
        }
        if (!sessionUserService.isSessionUserExists(userId)) {
            sendError(session, ErrorCode.TOKEN_INVALID, "登录状态已失效，请重新登录", clientMsgId);
            sessionManager.removeSession(session);
            return;
        }

        if (!validateSendRequest(session, request, clientMsgId)) {
            return;
        }

        try {
            WfMessage message = messageService.sendMessage(buildSendCommand(userId, request));

            MessageVO messageVO = buildMessageVOWithSender(message);
            sendAck(session, request.getClientMsgId(), messageVO);
            pushMessageToReceiver(message, messageVO);
        } catch (BaseException e) {
            sendError(session, e.getCode(), e.getMessage(), clientMsgId);
        } catch (Exception e) {
            log.error("WS 发送消息失败: {}", e.getMessage(), e);
            sendError(session, ErrorCode.SYSTEM_ERROR, "系统错误", clientMsgId);
        }
    }

    private boolean validateSendRequest(WebSocketSession session, WsRequest request, String clientMsgId) {
        if (request.getConversationId() == null) {
            sendError(session, ErrorCode.PARAM_ERROR, "会话ID不能为空", clientMsgId);
            return false;
        }

        return true;
    }

    private void sendAck(WebSocketSession session, String clientMsgId, MessageVO messageVO) {
        WsResponse ack = buildResponse(WsType.ACK, messageVO);
        ack.setClientMsgId(clientMsgId);
        send(session, ack);
    }

    private WsResponse buildMessageResponse(MessageVO messageVO) {
        return buildResponse(WsType.MESSAGE, messageVO);
    }

    private void pushMessageToReceiver(WfMessage message, MessageVO messageVO) {
        WsResponse push = buildMessageResponse(messageVO);
        int successCount = sessionManager.sendToUser(message.getReceiverId(), JSON.toJSONString(push));
        if (successCount > 0) {
            messageService.markDelivered(List.of(message.getId()));
            log.info("WS 消息送达: messageId={}, receiverId={}", message.getId(), message.getReceiverId());
        }
    }

    private boolean send(WebSocketSession session, WsResponse response) {
        try {
            session.sendMessage(new TextMessage(JSON.toJSONString(response)));
            return true;
        } catch (Exception e) {
            log.warn("WS 发送失败: sessionId={}, error={}", session.getId(), e.getMessage());
            return false;
        }
    }

    private void sendError(WebSocketSession session, ErrorCode errorCode, String message) {
        sendError(session, errorCode.getCode(), message, null);
    }

    private void sendError(WebSocketSession session, ErrorCode errorCode, String message, String clientMsgId) {
        sendError(session, errorCode.getCode(), message, clientMsgId);
    }

    private void sendError(WebSocketSession session, Integer code, String message, String clientMsgId) {
        WsResponse response = buildResponse(WsType.ERROR, null);
        response.setCode(code);
        response.setMessage(message);
        response.setClientMsgId(clientMsgId);
        send(session, response);
    }

    private WsResponse buildResponse(WsType type, Object data) {
        WsResponse response = new WsResponse();
        response.setType(type);
        response.setData(data);
        return response;
    }

    private MessageVO buildMessageVOWithSender(WfMessage message) {
        WfUser sender = userService.getByIdOrThrow(message.getSenderId());
        return messageMediaService.fillMedia(MessageConverter.toMessageVO(message, sender));
    }

    private SendMessageCommand buildSendCommand(Long userId, WsRequest request) {
        SendMessageCommand command = new SendMessageCommand();
        command.setUserId(userId);
        command.setConversationId(request.getConversationId());
        command.setContent(request.getContent());
        command.setMsgType(request.getMsgType());
        command.setMediaKey(request.getMediaKey());
        command.setMediaWidth(request.getMediaWidth());
        command.setMediaHeight(request.getMediaHeight());
        command.setMediaSize(request.getMediaSize());
        command.setMediaMimeType(request.getMediaMimeType());
        return command;
    }
}
