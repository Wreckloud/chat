package com.wreckloud.wolfchat.group.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * @Description 发布群公告DTO
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Data
@Schema(description = "发布群公告请求参数")
public class GroupNoticePublishDTO {

    @Schema(description = "公告标题", example = "班级通知")
    @Size(max = 100, message = "公告标题不能超过100个字符")
    private String title;

    @Schema(description = "公告内容", example = "下周一上午9点开班会，请准时参加", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "公告内容不能为空")
    @Size(max = 1000, message = "公告内容不能超过1000个字符")
    private String content;

    @Schema(description = "是否置顶", example = "true")
    private Boolean isPinned = false;
}

