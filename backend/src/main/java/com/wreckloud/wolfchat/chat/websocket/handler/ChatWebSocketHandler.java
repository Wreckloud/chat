package com.wreckloud.wolfchat.chat.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.chat.conversation.application.service.ConversationService;
import com.wreckloud.wolfchat.chat.conversation.domain.entity.WfConversation;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageType;
import com.wreckloud.wolfchat.chat.message.api.converter.MessageConverter;
import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.lobby.api.vo.LobbyMessageVO;
import com.wreckloud.wolfchat.chat.lobby.application.command.SendLobbyMessageCommand;
import com.wreckloud.wolfchat.chat.lobby.application.service.LobbyService;
import com.wreckloud.wolfchat.chat.message.application.command.SendMessageCommand;
import com.wreckloud.wolfchat.chat.message.application.service.ChatMessagePushService;
import com.wreckloud.wolfchat.chat.message.application.service.MessageMediaService;
import com.wreckloud.wolfchat.chat.message.application.service.MessageService;
import com.wreckloud.wolfchat.chat.message.domain.enums.MessageDeliveryStatus;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import com.wreckloud.wolfchat.chat.websocket.dto.WsRequest;
import com.wreckloud.wolfchat.chat.websocket.dto.WsResponse;
import com.wreckloud.wolfchat.chat.websocket.dto.UploadProgressPayload;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.chat.websocket.session.WsSessionManager;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.service.SessionUserService;
import com.wreckloud.wolfchat.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * @Description WebSocket 消息处理
 * @Author Wreckloud
 * @Date 2026-02-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final String SEND_DEDUP_KEY_PREFIX = "chat:ws:send:dedup:";
    private static final Duration SEND_DEDUP_TTL = Duration.ofMinutes(10);
    private static final String SEND_DEDUP_PENDING = "PENDING";
    private static final String SESSION_PASSWORD_VERSION_KEY = "session_pwd_ver";
    private static final String SEND_TYPE_PRIVATE = "SEND";
    private static final String SEND_TYPE_LOBBY = "LOBBY_SEND";
    private static final Set<String> ALLOWED_UPLOAD_STATUS = Set.of("UPLOADING", "SENDING", "FAILED");

    private final JwtUtil jwtUtil;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final LobbyService lobbyService;
    private final ChatMessagePushService chatMessagePushService;
    private final MessageMediaService messageMediaService;
    private final UserService userService;
    private final WsSessionManager sessionManager;
    private final SessionUserService sessionUserService;
    private final StringRedisTemplate stringRedisTemplate;
    private final Map<WsType, BiConsumer<WebSocketSession, WsRequest>> requestHandlerMap = new EnumMap<>(WsType.class);

    @PostConstruct
    public void initRequestHandlers() {
        requestHandlerMap.put(WsType.AUTH, this::handleAuth);
        requestHandlerMap.put(WsType.SEND, this::handleSend);
        requestHandlerMap.put(WsType.RECALL, this::handleRecall);
        requestHandlerMap.put(WsType.LOBBY_SEND, this::handleLobbySend);
        requestHandlerMap.put(WsType.LOBBY_RECALL, this::handleLobbyRecall);
        requestHandlerMap.put(WsType.UPLOAD_PROGRESS, this::handleUploadProgress);
        requestHandlerMap.put(WsType.PING, this::handlePingRequest);
    }

    @FunctionalInterface
    private interface AuthenticatedRequestHandler {
        void handle(Long userId, String clientMsgId, WsRequest request) throws Exception;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        log.debug("WS 连接建立: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        WsRequest request = parseRequest(session, message.getPayload());
        if (request == null) {
            return;
        }

        BiConsumer<WebSocketSession, WsRequest> requestHandler = requestHandlerMap.get(request.getType());
        if (requestHandler == null) {
            sendError(session, ErrorCode.PARAM_ERROR, "不支持的消息类型");
            return;
        }
        try {
            requestHandler.accept(session, request);
        } catch (BaseException e) {
            sendError(session, e.getCode(), e.getMessage(), request.getClientMsgId());
        } catch (Exception e) {
            log.error("WS 请求处理异常: type={}, sessionId={}",
                    request.getType(), session.getId(), e);
            sendError(session, ErrorCode.SYSTEM_ERROR, "系统错误", request.getClientMsgId());
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessionManager.removeSession(session);
        log.debug("WS 连接关闭: sessionId={}, status={}", session.getId(), status);
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
        log.debug("WS 认证成功: userId={}, sessionId={}", userId, session.getId());
        sendAuthOk(session);
        replayUndeliveredMessages(session, userId);
    }

    private void handlePingRequest(WebSocketSession session, WsRequest request) {
        withAuthenticatedUser(session, request, "心跳", (userId, clientMsgId, currentRequest) -> {
            sessionManager.refreshOnline(userId);
        });
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
        Long tokenPasswordVersion = jwtUtil.getPasswordVersionFromToken(token);
        if (tokenPasswordVersion == null) {
            sendError(session, ErrorCode.TOKEN_INVALID, "token 无效");
            return null;
        }
        if (!sessionUserService.isSessionUserExists(userId)) {
            sendError(session, ErrorCode.TOKEN_INVALID, "token 无效");
            return null;
        }
        if (!sessionUserService.isPasswordVersionMatched(userId, tokenPasswordVersion)) {
            sendError(session, ErrorCode.TOKEN_INVALID, "token 无效");
            return null;
        }
        session.getAttributes().put(SESSION_PASSWORD_VERSION_KEY, tokenPasswordVersion);
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
        withAuthenticatedUser(session, request, "发送消息", (userId, clientMsgId, currentRequest) -> {
            if (!validateSendRequest(session, currentRequest, clientMsgId)) {
                return;
            }
            String dedupKey = buildSendDedupKey(WsType.SEND, userId, clientMsgId);
            MessageVO duplicateMessage = resolveDuplicatePrivateMessageVO(session, clientMsgId, dedupKey);
            if (duplicateMessage != null) {
                return;
            }
            if (!tryMarkSendDedupPending(dedupKey)) {
                sendError(session, ErrorCode.PARAM_ERROR, "请求处理中，请勿重复发送", clientMsgId);
                return;
            }
            try {
                WfMessage message = messageService.sendMessage(buildSendCommand(userId, currentRequest));
                MessageVO messageVO = buildMessageVOWithSender(message);
                sendAck(session, clientMsgId, messageVO);
                pushMessageToReceiver(message, messageVO);
                storeSendDedupMessageId(dedupKey, message.getId());
            } catch (Exception e) {
                clearSendDedupPending(dedupKey);
                throw e;
            }
        });
    }

    private void handleLobbySend(WebSocketSession session, WsRequest request) {
        withAuthenticatedUser(session, request, "发送大厅消息", (userId, clientMsgId, currentRequest) -> {
            String dedupKey = buildSendDedupKey(WsType.LOBBY_SEND, userId, clientMsgId);
            LobbyMessageVO duplicateMessage = resolveDuplicateLobbyMessageVO(session, clientMsgId, dedupKey);
            if (duplicateMessage != null) {
                return;
            }
            if (!tryMarkSendDedupPending(dedupKey)) {
                sendError(session, ErrorCode.PARAM_ERROR, "请求处理中，请勿重复发送", clientMsgId);
                return;
            }
            try {
                LobbyMessageVO messageVO = lobbyService.sendMessage(buildLobbySendCommand(userId, currentRequest));
                sendAck(session, clientMsgId, messageVO);
                pushLobbyMessage(userId, messageVO);
                storeSendDedupMessageId(dedupKey, messageVO.getMessageId());
            } catch (Exception e) {
                clearSendDedupPending(dedupKey);
                throw e;
            }
        });
    }

    private void handleLobbyRecall(WebSocketSession session, WsRequest request) {
        withAuthenticatedUser(session, request, "撤回大厅消息", (userId, clientMsgId, currentRequest) -> {
            if (!validateLobbyRecallRequest(session, currentRequest, clientMsgId)) {
                return;
            }
            LobbyMessageVO messageVO = lobbyService.recallMessage(userId, currentRequest.getMessageId());
            sendAck(session, clientMsgId, messageVO);
            pushLobbyMessage(userId, messageVO);
        });
    }

    private void handleRecall(WebSocketSession session, WsRequest request) {
        withAuthenticatedUser(session, request, "撤回消息", (userId, clientMsgId, currentRequest) -> {
            if (!validateRecallRequest(session, currentRequest, clientMsgId)) {
                return;
            }
            WfMessage message = messageService.recallMessage(
                    userId,
                    currentRequest.getConversationId(),
                    currentRequest.getMessageId()
            );
            MessageVO messageVO = buildMessageVOWithSender(message);
            sendAck(session, clientMsgId, messageVO);
            pushMessageToConversationPeers(messageVO, message.getSenderId(), message.getReceiverId());
        });
    }

    private void handleUploadProgress(WebSocketSession session, WsRequest request) {
        withAuthenticatedUser(session, request, "同步上传进度", (userId, clientMsgId, currentRequest) -> {
            if (!validateUploadProgressRequest(session, currentRequest, clientMsgId)) {
                return;
            }
            String sendType = currentRequest.getSendType().trim().toUpperCase();
            UploadProgressPayload payload = buildUploadProgressPayload(userId, currentRequest, sendType);
            WsResponse response = buildResponse(WsType.UPLOAD_PROGRESS, payload);
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

    private void withAuthenticatedUser(WebSocketSession session,
                                       WsRequest request,
                                       String scene,
                                       AuthenticatedRequestHandler handler) {
        String clientMsgId = request.getClientMsgId();
        Long userId = sessionManager.getUserId(session);
        if (userId == null) {
            sendError(session, ErrorCode.UNAUTHORIZED, "请先认证", clientMsgId);
            return;
        }

        try {
            if (!sessionUserService.isSessionUserExists(userId)) {
                sendError(session, ErrorCode.TOKEN_INVALID, "登录状态已失效，请重新登录", clientMsgId);
                sessionManager.removeSession(session);
                return;
            }
            Long tokenPasswordVersion = (Long) session.getAttributes().get(SESSION_PASSWORD_VERSION_KEY);
            if (!sessionUserService.isPasswordVersionMatched(userId, tokenPasswordVersion)) {
                sendError(session, ErrorCode.TOKEN_INVALID, "登录状态已失效，请重新登录", clientMsgId);
                sessionManager.removeSession(session);
                return;
            }
            handler.handle(userId, clientMsgId, request);
        } catch (BaseException e) {
            sendError(session, e.getCode(), e.getMessage(), clientMsgId);
        } catch (Exception e) {
            log.error("WS {}失败: userId={}, sessionId={}", scene, userId, session.getId(), e);
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

    private boolean validateRecallRequest(WebSocketSession session, WsRequest request, String clientMsgId) {
        if (request.getConversationId() == null) {
            sendError(session, ErrorCode.PARAM_ERROR, "会话ID不能为空", clientMsgId);
            return false;
        }
        if (request.getMessageId() == null || request.getMessageId() <= 0L) {
            sendError(session, ErrorCode.PARAM_ERROR, "消息ID不能为空", clientMsgId);
            return false;
        }
        return true;
    }

    private boolean validateLobbyRecallRequest(WebSocketSession session, WsRequest request, String clientMsgId) {
        if (request.getMessageId() == null || request.getMessageId() <= 0L) {
            sendError(session, ErrorCode.PARAM_ERROR, "消息ID不能为空", clientMsgId);
            return false;
        }
        return true;
    }

    private boolean validateUploadProgressRequest(WebSocketSession session, WsRequest request, String clientMsgId) {
        String sendType = request.getSendType() == null ? "" : request.getSendType().trim().toUpperCase();
        if (!SEND_TYPE_PRIVATE.equals(sendType) && !SEND_TYPE_LOBBY.equals(sendType)) {
            sendError(session, ErrorCode.PARAM_ERROR, "发送场景不正确", clientMsgId);
            return false;
        }
        if (!StringUtils.hasText(request.getClientMsgId())) {
            sendError(session, ErrorCode.PARAM_ERROR, "客户端消息ID不能为空", clientMsgId);
            return false;
        }
        MessageType msgType = request.getMsgType();
        if (!MessageType.IMAGE.equals(msgType)
                && !MessageType.VIDEO.equals(msgType)
                && !MessageType.FILE.equals(msgType)) {
            sendError(session, ErrorCode.PARAM_ERROR, "消息类型不支持进度同步", clientMsgId);
            return false;
        }
        int progress = request.getUploadProgress() == null ? -1 : request.getUploadProgress();
        if (progress < 0 || progress > 100) {
            sendError(session, ErrorCode.PARAM_ERROR, "上传进度不合法", clientMsgId);
            return false;
        }
        String uploadStatus = request.getUploadStatus() == null ? "" : request.getUploadStatus().trim().toUpperCase();
        if (!ALLOWED_UPLOAD_STATUS.contains(uploadStatus)) {
            sendError(session, ErrorCode.PARAM_ERROR, "上传状态不合法", clientMsgId);
            return false;
        }

        if (SEND_TYPE_PRIVATE.equals(sendType)) {
            if (request.getConversationId() == null || request.getConversationId() <= 0L) {
                sendError(session, ErrorCode.PARAM_ERROR, "会话ID不能为空", clientMsgId);
                return false;
            }
            Long userId = sessionManager.getUserId(session);
            if (userId == null) {
                sendError(session, ErrorCode.UNAUTHORIZED, "请先认证", clientMsgId);
                return false;
            }
            conversationService.validateConversationMember(request.getConversationId(), userId);
        }
        return true;
    }

    private UploadProgressPayload buildUploadProgressPayload(Long userId, WsRequest request, String sendType) {
        WfUser sender = userService.getByIdOrThrow(userId);
        UploadProgressPayload payload = new UploadProgressPayload();
        payload.setSendType(sendType);
        payload.setConversationId(request.getConversationId());
        payload.setClientMsgId(request.getClientMsgId());
        payload.setSenderId(userId);
        payload.setSenderWolfNo(sender.getWolfNo());
        payload.setSenderNickname(sender.getNickname());
        payload.setSenderEquippedTitleName(sender.getEquippedTitleName());
        payload.setSenderEquippedTitleColor(sender.getEquippedTitleColor());
        payload.setSenderAvatar(sender.getAvatar());
        payload.setMsgType(request.getMsgType());
        payload.setUploadProgress(request.getUploadProgress());
        payload.setUploadStatus(request.getUploadStatus() == null ? null : request.getUploadStatus().trim().toUpperCase());
        payload.setCreateTime(LocalDateTime.now());
        return payload;
    }

    private void sendAck(WebSocketSession session, String clientMsgId, Object data) {
        WsResponse ack = buildResponse(WsType.ACK, data);
        ack.setClientMsgId(clientMsgId);
        send(session, ack);
    }

    private WsResponse buildMessageResponse(MessageVO messageVO) {
        return buildResponse(WsType.MESSAGE, messageVO);
    }

    private void pushMessageToReceiver(WfMessage message, MessageVO messageVO) {
        chatMessagePushService.pushPrivateMessageToReceiver(message);
    }

    private void pushLobbyMessage(Long senderUserId, LobbyMessageVO messageVO) {
        chatMessagePushService.pushLobbyMessage(senderUserId, messageVO);
    }

    private void pushMessageToConversationPeers(MessageVO messageVO, Long senderUserId, Long receiverUserId) {
        chatMessagePushService.pushPrivateMessageToConversationPeers(messageVO, senderUserId, receiverUserId);
    }

    private String buildSendDedupKey(WsType wsType, Long userId, String clientMsgId) {
        if (!StringUtils.hasText(clientMsgId) || userId == null || wsType == null) {
            return null;
        }
        return SEND_DEDUP_KEY_PREFIX + wsType.name() + ":" + userId + ":" + clientMsgId.trim();
    }

    private Long getSendDedupMessageId(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(raw) || SEND_DEDUP_PENDING.equals(raw)) {
            return null;
        }
        try {
            long parsed = Long.parseLong(raw);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean tryMarkSendDedupPending(String key) {
        if (!StringUtils.hasText(key)) {
            return true;
        }
        Boolean created = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, SEND_DEDUP_PENDING, SEND_DEDUP_TTL);
        return Boolean.TRUE.equals(created);
    }

    private void clearSendDedupPending(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        String current = stringRedisTemplate.opsForValue().get(key);
        if (SEND_DEDUP_PENDING.equals(current)) {
            stringRedisTemplate.delete(key);
        }
    }

    private void storeSendDedupMessageId(String key, Long messageId) {
        if (!StringUtils.hasText(key) || messageId == null || messageId <= 0L) {
            return;
        }
        stringRedisTemplate.opsForValue().set(key, String.valueOf(messageId), SEND_DEDUP_TTL);
    }

    private MessageVO resolveDuplicatePrivateMessageVO(WebSocketSession session, String clientMsgId, String dedupKey) {
        Long duplicateMessageId = getSendDedupMessageId(dedupKey);
        if (duplicateMessageId == null) {
            return null;
        }
        WfMessage duplicateMessage = messageService.getById(duplicateMessageId);
        if (duplicateMessage == null) {
            stringRedisTemplate.delete(dedupKey);
            return null;
        }

        MessageVO messageVO = buildMessageVOWithSender(duplicateMessage);
        sendAck(session, clientMsgId, messageVO);
        if (MessageDeliveryStatus.UNDELIVERED.equals(duplicateMessage.getDelivered())) {
            pushMessageToReceiver(duplicateMessage, messageVO);
        }
        return messageVO;
    }

    private LobbyMessageVO resolveDuplicateLobbyMessageVO(WebSocketSession session, String clientMsgId, String dedupKey) {
        Long duplicateMessageId = getSendDedupMessageId(dedupKey);
        if (duplicateMessageId == null) {
            return null;
        }
        LobbyMessageVO messageVO = lobbyService.getMessageById(duplicateMessageId);
        if (messageVO == null) {
            stringRedisTemplate.delete(dedupKey);
            return null;
        }
        sendAck(session, clientMsgId, messageVO);
        return messageVO;
    }

    private boolean send(WebSocketSession session, WsResponse response) {
        try {
            session.sendMessage(new TextMessage(JSON.toJSONString(response)));
            return true;
        } catch (Exception e) {
            log.warn("WS 发送失败: sessionId={}", session.getId(), e);
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
        WfUser sender = null;
        Long senderId = message.getSenderId();
        if (senderId != null && senderId > 0L) {
            sender = userService.getByIdOrThrow(senderId);
        }
        return messageMediaService.fillMedia(MessageConverter.toMessageVO(message, sender));
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

    private SendLobbyMessageCommand buildLobbySendCommand(Long userId, WsRequest request) {
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
