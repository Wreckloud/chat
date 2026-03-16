package com.wreckloud.wolfchat.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 管理端创建封禁请求。
 */
@Data
@Schema(description = "管理端创建封禁请求")
public class CreateUserBanDTO {
    @NotBlank(message = "封禁原因不能为空")
    @Size(max = 500, message = "封禁原因不能超过500字符")
    @Schema(description = "封禁原因", required = true)
    private String reason;

    @Min(value = 1, message = "封禁时长必须大于0小时")
    @Schema(description = "封禁小时数；不传表示永久封禁")
    private Integer durationHours;
}

