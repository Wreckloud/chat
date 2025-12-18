package com.wreckloud.wolfchat.group.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @Description 禁言设置DTO
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Data
@Schema(description = "禁言设置请求参数")
public class GroupMuteDTO {

    @Schema(description = "是否禁言", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "禁言状态不能为空")
    private Boolean isMuted;

    @Schema(description = "禁言时长（秒），0表示永久禁言，null表示取消禁言", example = "86400")
    private Long muteDuration;
}

