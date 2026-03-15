package com.wreckloud.wolfchat.community.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * @Description 发布主题 DTO
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@Schema(description = "发布主题请求")
public class CreateThreadDTO {
    @NotBlank(message = "主题标题不能为空")
    @Size(max = 120, message = "主题标题长度不能超过120个字符")
    @Schema(description = "主题标题")
    private String title;

    @NotBlank(message = "主题内容不能为空")
    @Size(max = 5000, message = "主题内容长度不能超过5000个字符")
    @Schema(description = "主题内容")
    private String content;
}
