package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @Description 号码池生成请求DTO
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Data
@Schema(description = "号码池生成请求")
public class NoPoolGenerateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "生成数量", required = true, example = "100", minimum = "1", maximum = "1000")
    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "生成数量至少为1")
    private Integer count;
}

