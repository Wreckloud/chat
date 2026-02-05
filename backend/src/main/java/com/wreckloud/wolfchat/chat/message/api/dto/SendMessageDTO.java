package com.wreckloud.wolfchat.chat.message.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Description 发送消息 DTO
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Data
@Schema(description = "发送消息请求")
public class SendMessageDTO {
    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息内容")
    private String content;
}

