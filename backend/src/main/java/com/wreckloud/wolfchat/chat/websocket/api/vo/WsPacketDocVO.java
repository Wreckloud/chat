package com.wreckloud.wolfchat.chat.websocket.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @Description WebSocket 报文文档 VO
 * @Author Wreckloud
 * @Date 2026-03-30
 */
@Data
@Schema(description = "WebSocket 报文文档")
public class WsPacketDocVO {
    @Schema(description = "报文类型（对应 WsType）", example = "SEND")
    private String type;

    @Schema(description = "使用场景", example = "客户端发送私聊消息")
    private String scene;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "字段列表")
    private List<WsPacketFieldDocVO> fields;

    @Schema(description = "示例 JSON")
    private String sampleJson;
}

