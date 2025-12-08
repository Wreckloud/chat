package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @Description 号码池更新请求DTO
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Data
@Schema(description = "号码池更新请求")
public class NoPoolUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "号码ID", required = true, example = "1")
    @NotNull(message = "号码ID不能为空")
    private Long id;

    @Schema(description = "状态：0未使用 1已使用 2冻结", example = "0")
    private Integer status;

    @Schema(description = "是否靓号", example = "false")
    private Boolean isPretty;

    @Schema(description = "用户ID（设为null可释放号码）", example = "1")
    private Long userId;
}

