package com.wreckloud.wolfchat.chat.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.wreckloud.wolfchat.chat.message.api.converter.MessageConverter;
import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.message.application.service.MessageService;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.chat.websocket.dto.WsRequest;
import com.wreckloud.wolfchat.chat.websocket.dto.WsResponse;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.chat.websocket.session.WsSessionManager;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
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
    private final WsSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        log.info("WS 连接建立: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        WsRequest request;
        try {
            request = JSON.parseObject(message.getPayload(), WsRequest.class);
        } catch (Exception e) {
            sendError(session, ErrorCode.PARAM_ERROR, "消息格式错误");
            return;
        }

        if (request == null || request.getType() == null) {
            sendError(session, ErrorCode.PARAM_ERROR, "消息类型不能为空");
            return;
        }

        switch (request.getType()) {
            case AUTH:
                handleAuth(session, request);
                break;
            case SEND:
                handleSend(session, request);
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

    private void handleAuth(WebSocketSession session, WsRequest request) {
        String token = request.getToken();
        if (!StringUtils.hasText(token)) {
            sendError(session, ErrorCode.UNAUTHORIZED, "token 不能为空");
            return;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!jwtUtil.validateToken(token)) {
            sendError(session, ErrorCode.TOKEN_INVALID, "token 无效");
            return;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            sendError(session, ErrorCode.TOKEN_INVALID, "token 无效");
            return;
        }

        sessionManager.addSession(userId, session);
        log.info("WS 认证成功: userId={}, sessionId={}", userId, session.getId());
        WsResponse response = new WsResponse();
        response.setType(WsType.AUTH_OK);
        send(session, response);

        // 推送未送达消息
        List<WfMessage> undelivered = messageService.listUndeliveredMessages(userId);
        if (!undelivered.isEmpty()) {
            log.info("WS 补发未送达消息: userId={}, count={}", userId, undelivered.size());
            List<Long> deliveredIds = new ArrayList<>();
            for (WfMessage message : undelivered) {
                WsResponse push = new WsResponse();
                push.setType(WsType.MESSAGE);
                push.setData(MessageConverter.toMessageVO(message));
                if (send(session, push)) {
                    deliveredIds.add(message.getId());
                }
            }
            if (!deliveredIds.isEmpty()) {
                messageService.markDelivered(deliveredIds);
            }
        }
    }

    private void handleSend(WebSocketSession session, WsRequest request) {
        String clientMsgId = request.getClientMsgId();
        Long userId = sessionManager.getUserId(session);
        if (userId == null) {
            sendError(session, ErrorCode.UNAUTHORIZED, "请先认证", clientMsgId);
            return;
        }

        if (request.getConversationId() == null) {
            sendError(session, ErrorCode.PARAM_ERROR, "会话ID不能为空", clientMsgId);
            return;
        }

        if (!StringUtils.hasText(request.getContent())) {
            sendError(session, ErrorCode.MESSAGE_CONTENT_EMPTY, "消息内容不能为空", clientMsgId);
            return;
        }

        try {
            WfMessage message = messageService.sendMessage(
                    userId,
                    request.getConversationId(),
                    request.getContent()
            );

            MessageVO messageVO = MessageConverter.toMessageVO(message);

            WsResponse ack = new WsResponse();
            ack.setType(WsType.ACK);
            ack.setClientMsgId(request.getClientMsgId());
            ack.setData(messageVO);
            send(session, ack);

            WsResponse push = new WsResponse();
            push.setType(WsType.MESSAGE);
            push.setData(messageVO);
            int successCount = sessionManager.sendToUser(message.getReceiverId(), JSON.toJSONString(push));
            if (successCount > 0) {
                messageService.markDelivered(List.of(message.getId()));
                log.info("WS 消息送达: messageId={}, receiverId={}", message.getId(), message.getReceiverId());
            }
        } catch (BaseException e) {
            sendError(session, e.getCode(), e.getMessage(), clientMsgId);
        } catch (Exception e) {
            log.error("WS 发送消息失败: {}", e.getMessage(), e);
            sendError(session, ErrorCode.SYSTEM_ERROR, "系统错误", clientMsgId);
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

    private void sendError(WebSocketSession session, Integer code, String message) {
        sendError(session, code, message, null);
    }

    private void sendError(WebSocketSession session, Integer code, String message, String clientMsgId) {
        WsResponse response = new WsResponse();
        response.setType(WsType.ERROR);
        response.setCode(code);
        response.setMessage(message);
        response.setClientMsgId(clientMsgId);
        send(session, response);
    }
}
