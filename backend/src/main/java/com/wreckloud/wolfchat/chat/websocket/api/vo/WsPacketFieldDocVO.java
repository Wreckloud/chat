package com.wreckloud.wolfchat.chat.websocket.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description WebSocket 报文字段文档 VO
 * @Author Wreckloud
 * @Date 2026-03-30
 */
@Data
@Schema(description = "WebSocket 报文字段文档")
public class WsPacketFieldDocVO {
    @Schema(description = "字段名", example = "clientMsgId")
    private String name;

    @Schema(description = "字段类型", example = "String")
    private String type;

    @Schema(description = "是否必填")
    private Boolean required;

    @Schema(description = "字段说明")
    private String description;
}

