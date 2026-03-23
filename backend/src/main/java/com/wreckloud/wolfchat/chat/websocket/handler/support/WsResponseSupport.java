package com.wreckloud.wolfchat.chat.websocket.handler.support;

import com.alibaba.fastjson.JSON;
import com.wreckloud.wolfchat.chat.message.api.vo.MessageVO;
import com.wreckloud.wolfchat.chat.websocket.dto.WsResponse;
import com.wreckloud.wolfchat.chat.websocket.enums.WsType;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * @Description WebSocket 响应发送与错误回包支持
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Slf4j
@Component
public class WsResponseSupport {

    public void sendAuthOk(WebSocketSession session) {
        send(session, buildResponse(WsType.AUTH_OK, null));
    }

    public void sendAck(WebSocketSession session, String clientMsgId, Object data) {
        WsResponse ack = buildResponse(WsType.ACK, data);
        ack.setClientMsgId(clientMsgId);
        send(session, ack);
    }

    public WsResponse buildMessageResponse(MessageVO messageVO) {
        return buildResponse(WsType.MESSAGE, messageVO);
    }

    public void sendError(WebSocketSession session, ErrorCode errorCode, String message) {
        sendError(session, errorCode.getCode(), message, null);
    }

    public void sendError(WebSocketSession session, ErrorCode errorCode, String message, String clientMsgId) {
        sendError(session, errorCode.getCode(), message, clientMsgId);
    }

    public void sendError(WebSocketSession session, Integer code, String message, String clientMsgId) {
        WsResponse response = buildResponse(WsType.ERROR, null);
        response.setCode(code);
        response.setMessage(message);
        response.setClientMsgId(clientMsgId);
        send(session, response);
    }

    public WsResponse buildResponse(WsType type, Object data) {
        WsResponse response = new WsResponse();
        response.setType(type);
        response.setData(data);
        return response;
    }

    public boolean send(WebSocketSession session, WsResponse response) {
        try {
            session.sendMessage(new TextMessage(JSON.toJSONString(response)));
            return true;
        } catch (Exception e) {
            log.warn("WS 发送失败: sessionId={}", session.getId(), e);
            return false;
        }
    }
}

