package com.wreckloud.wolfchat.group.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @Description 设置群成员角色DTO
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Data
@Schema(description = "设置群成员角色请求参数")
public class GroupMemberRoleDTO {

    @Schema(description = "目标用户ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "是否设置为管理员（true=设置，false=取消）", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "角色设置不能为空")
    private Boolean isAdmin;
}

