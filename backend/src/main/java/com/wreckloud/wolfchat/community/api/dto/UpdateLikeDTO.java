package com.wreckloud.wolfchat.community.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @Description 点赞状态更新 DTO
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@Schema(description = "点赞状态更新参数")
public class UpdateLikeDTO {
    @NotNull(message = "liked 不能为空")
    @Schema(description = "是否点赞：true-点赞，false-取消点赞", required = true)
    private Boolean liked;
}
