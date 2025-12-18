package com.wreckloud.wolfchat.group.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @Description 创建群组DTO
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Data
@Schema(description = "创建群组请求参数")
public class GroupCreateDTO {

    @Schema(description = "群名称", example = "2022级计科3班", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "群名称不能为空")
    @Size(max = 50, message = "群名称不能超过50个字符")
    private String groupName;

    @Schema(description = "群头像URL", example = "https://example.com/avatar.jpg")
    @Size(max = 255, message = "群头像URL不能超过255个字符")
    private String groupAvatar;

    @Schema(description = "群简介", example = "我们是最棒的班级")
    @Size(max = 500, message = "群简介不能超过500个字符")
    private String groupIntro;

    @Schema(description = "初始成员用户ID列表（不包含创建者）", example = "[2, 3, 4]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "成员列表不能为空")
    @Size(min = 1, message = "至少需要邀请1个成员")
    private List<Long> memberIds;

    @Schema(description = "群人数上限", example = "200")
    private Integer maxMembers = 200;
}

