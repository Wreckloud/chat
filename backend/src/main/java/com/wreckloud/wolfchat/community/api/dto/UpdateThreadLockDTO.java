package com.wreckloud.wolfchat.community.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @Description 主题锁定状态更新 DTO
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@Schema(description = "主题锁定状态更新请求")
public class UpdateThreadLockDTO {
    @NotNull(message = "锁定状态不能为空")
    @Schema(description = "是否锁定：true-锁定，false-解锁")
    private Boolean locked;
}
