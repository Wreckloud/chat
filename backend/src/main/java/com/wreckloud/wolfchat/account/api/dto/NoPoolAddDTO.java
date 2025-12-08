package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @Description 管理员手动添加号码请求DTO
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Data
@Schema(description = "管理员手动添加号码请求")
public class NoPoolAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "号码（6-10位数）", required = true, example = "1000001", minimum = "100000", maximum = "9999999999")
    @NotNull(message = "号码不能为空")
    @Min(value = 100000, message = "号码必须大于等于100000（6位数）")
    private Long wfNo;

    @Schema(description = "是否靓号", example = "false")
    private Boolean isPretty = false;
}

