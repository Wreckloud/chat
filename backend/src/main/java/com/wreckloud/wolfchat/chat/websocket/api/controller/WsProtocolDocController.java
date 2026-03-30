package com.wreckloud.wolfchat.chat.websocket.api.controller;

import com.wreckloud.wolfchat.chat.websocket.api.vo.WsPacketDocVO;
import com.wreckloud.wolfchat.chat.websocket.api.vo.WsPacketFieldDocVO;
import com.wreckloud.wolfchat.chat.websocket.api.vo.WsProtocolDocVO;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Description WebSocket 协议文档控制器
 * @Author Wreckloud
 * @Date 2026-03-30
 */
@Tag(name = "聊天-WS协议", description = "补充 HTTP 文档之外的 WebSocket 协议说明")
@RestController
@RequestMapping("/ws-docs")
public class WsProtocolDocController {

    @Operation(summary = "WebSocket 协议文档", description = "返回 AUTH/SEND/ACK/MESSAGE/ERROR 等核心报文结构")
    @GetMapping("/protocol")
    public Result<WsProtocolDocVO> getProtocolDoc() {
        WsProtocolDocVO vo = new WsProtocolDocVO();
        vo.setWsPath("/api/ws/chat");
        vo.setAuthFlow("连接建立后先发送 AUTH(token)，收到 AUTH_OK 才允许发送业务消息。");
        vo.setAckFlow("客户端发送 SEND(clientMsgId) 后，服务端返回 ACK(clientMsgId) 或 ERROR(clientMsgId)。");
        vo.setRequestPackets(buildRequestPackets());
        vo.setResponsePackets(buildResponsePackets());
        return Result.success(vo);
    }

    private List<WsPacketDocVO> buildRequestPackets() {
        return List.of(
                packet(
                        "AUTH",
                        "连接鉴权",
                        "建立连接后立即发送，认证当前用户。",
                        List.of(
                                field("type", "String", true, "固定为 AUTH"),
                                field("token", "String", true, "登录接口返回的 JWT")
                        ),
                        "{\n  \"type\": \"AUTH\",\n  \"token\": \"eyJhbGciOiJIUzI1NiJ9...\"\n}"
                ),
                packet(
                        "SEND",
                        "私聊发送消息",
                        "发送文本/图片/视频/文件消息。",
                        List.of(
                                field("type", "String", true, "固定为 SEND"),
                                field("clientMsgId", "String", true, "客户端消息唯一标识，用于 ACK 对齐"),
                                field("conversationId", "Long", true, "会话ID"),
                                field("msgType", "String", true, "TEXT/IMAGE/VIDEO/FILE"),
                                field("content", "String", false, "文本内容，TEXT 时通常必填"),
                                field("mediaKey", "String", false, "媒体对象 Key"),
                                field("mediaPosterKey", "String", false, "视频封面 Key"),
                                field("replyToMessageId", "Long", false, "回复目标消息ID")
                        ),
                        "{\n  \"type\": \"SEND\",\n  \"clientMsgId\": \"cmsg_1743320101\",\n  \"conversationId\": 10001,\n  \"msgType\": \"TEXT\",\n  \"content\": \"今晚图书馆见？\"\n}"
                ),
                packet(
                        "PING",
                        "心跳",
                        "客户端可按需发送心跳保持连接。",
                        List.of(
                                field("type", "String", true, "固定为 PING")
                        ),
                        "{\n  \"type\": \"PING\"\n}"
                )
        );
    }

    private List<WsPacketDocVO> buildResponsePackets() {
        return List.of(
                packet(
                        "AUTH_OK",
                        "认证成功",
                        "服务端认证通过后返回。",
                        List.of(
                                field("type", "String", true, "固定为 AUTH_OK")
                        ),
                        "{\n  \"type\": \"AUTH_OK\"\n}"
                ),
                packet(
                        "ACK",
                        "发送回执",
                        "发送成功回执，clientMsgId 与请求一一对应。",
                        List.of(
                                field("type", "String", true, "固定为 ACK"),
                                field("clientMsgId", "String", true, "与请求中的 clientMsgId 一致"),
                                field("data", "Object", true, "消息体（MessageVO）")
                        ),
                        "{\n  \"type\": \"ACK\",\n  \"clientMsgId\": \"cmsg_1743320101\",\n  \"data\": {\n    \"messageId\": 600001,\n    \"conversationId\": 10001,\n    \"msgType\": \"TEXT\",\n    \"content\": \"今晚图书馆见？\"\n  }\n}"
                ),
                packet(
                        "MESSAGE",
                        "消息推送",
                        "服务端向接收方推送新消息。",
                        List.of(
                                field("type", "String", true, "固定为 MESSAGE"),
                                field("data", "Object", true, "消息体（MessageVO）")
                        ),
                        "{\n  \"type\": \"MESSAGE\",\n  \"data\": {\n    \"messageId\": 600001,\n    \"senderId\": 1,\n    \"senderNickname\": \"雲之残骸\",\n    \"msgType\": \"TEXT\",\n    \"content\": \"今晚图书馆见？\"\n  }\n}"
                ),
                packet(
                        "ERROR",
                        "错误回包",
                        "参数错误、权限错误、业务异常时返回。",
                        List.of(
                                field("type", "String", true, "固定为 ERROR"),
                                field("code", "Integer", true, "业务错误码"),
                                field("message", "String", true, "错误提示"),
                                field("clientMsgId", "String", false, "若由 SEND 触发，返回对应 clientMsgId")
                        ),
                        "{\n  \"type\": \"ERROR\",\n  \"code\": 1001,\n  \"message\": \"参数不合法\",\n  \"clientMsgId\": \"cmsg_1743320101\"\n}"
                )
        );
    }

    private WsPacketDocVO packet(String type,
                                 String scene,
                                 String description,
                                 List<WsPacketFieldDocVO> fields,
                                 String sampleJson) {
        WsPacketDocVO vo = new WsPacketDocVO();
        vo.setType(type);
        vo.setScene(scene);
        vo.setDescription(description);
        vo.setFields(fields);
        vo.setSampleJson(sampleJson);
        return vo;
    }

    private WsPacketFieldDocVO field(String name, String type, boolean required, String description) {
        WsPacketFieldDocVO vo = new WsPacketFieldDocVO();
        vo.setName(name);
        vo.setType(type);
        vo.setRequired(required);
        vo.setDescription(description);
        return vo;
    }
}

