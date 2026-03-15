package com.wreckloud.wolfchat.community.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @Description 主题精华状态更新 DTO
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@Schema(description = "主题精华状态更新请求")
public class UpdateThreadEssenceDTO {
    @NotNull(message = "精华状态不能为空")
    @Schema(description = "是否精华：true-设为精华，false-取消精华")
    private Boolean essence;
}
