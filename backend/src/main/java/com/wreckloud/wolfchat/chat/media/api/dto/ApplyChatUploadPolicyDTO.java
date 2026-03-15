package com.wreckloud.wolfchat.chat.media.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * @Description 申请聊天媒体上传策略 DTO
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@Schema(description = "申请聊天媒体上传策略")
public class ApplyChatUploadPolicyDTO {
    @javax.validation.constraints.NotBlank(message = "文件后缀不能为空")
    @Schema(description = "文件后缀，例如 jpg/mp4/pdf")
    private String extension;

    @Schema(description = "文件 MIME 类型（可选）")
    private String mimeType;

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小不合法")
    @Schema(description = "文件大小（字节）")
    private Long size;
}
