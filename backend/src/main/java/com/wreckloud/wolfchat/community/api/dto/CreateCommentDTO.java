package com.wreckloud.wolfchat.community.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Description 发布评论 DTO
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Data
@Schema(description = "发布评论请求")
public class CreateCommentDTO {
    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容")
    private String content;
}
