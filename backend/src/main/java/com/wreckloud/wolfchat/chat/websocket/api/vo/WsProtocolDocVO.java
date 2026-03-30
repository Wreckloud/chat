package com.wreckloud.wolfchat.chat.websocket.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @Description WebSocket 协议文档 VO
 * @Author Wreckloud
 * @Date 2026-03-30
 */
@Data
@Schema(description = "WebSocket 协议文档")
public class WsProtocolDocVO {
    @Schema(description = "WebSocket 连接路径", example = "/api/ws/chat")
    private String wsPath;

    @Schema(description = "认证流程说明")
    private String authFlow;

    @Schema(description = "消息确认流程说明")
    private String ackFlow;

    @Schema(description = "客户端请求报文")
    private List<WsPacketDocVO> requestPackets;

    @Schema(description = "服务端响应报文")
    private List<WsPacketDocVO> responsePackets;
}

