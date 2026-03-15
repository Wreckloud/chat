package com.wreckloud.wolfchat.community.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @Description 主题置顶状态更新 DTO
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@Schema(description = "主题置顶状态更新请求")
public class UpdateThreadStickyDTO {
    @NotNull(message = "置顶状态不能为空")
    @Schema(description = "是否置顶：true-置顶，false-取消置顶")
    private Boolean sticky;
}
