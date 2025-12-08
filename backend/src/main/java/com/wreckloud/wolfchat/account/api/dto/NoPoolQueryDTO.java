package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description 号码池查询请求DTO
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Data
@Schema(description = "号码池查询请求")
public class NoPoolQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码，从1开始", example = "1")
    private Integer page = 1;

    @Schema(description = "每页数量", example = "10")
    private Integer size = 10;

    @Schema(description = "号码", example = "1000001")
    private Long wfNo;

    @Schema(description = "状态：0未使用 1已使用 2冻结", example = "0")
    private Integer status;

    @Schema(description = "是否靓号", example = "false")
    private Boolean isPretty;

    @Schema(description = "用户ID", example = "1")
    private Long userId;
}

