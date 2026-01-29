package com.wreckloud.wolfchat.community.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Description 发布帖子 DTO
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Data
@Schema(description = "发布帖子请求")
public class CreatePostDTO {
    @NotBlank(message = "帖子内容不能为空")
    @Schema(description = "帖子内容")
    private String content;

    @Schema(description = "关联聊天室ID（可选）")
    private Long roomId;
}
