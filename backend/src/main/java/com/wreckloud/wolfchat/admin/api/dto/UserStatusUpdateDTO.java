package com.wreckloud.wolfchat.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @Description 管理员更新用户状态DTO
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Data
@Schema(description = "更新用户状态请求")
public class UserStatusUpdateDTO {
    
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", required = true)
    private Long userId;
    
    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：1=正常 2=禁用 3=注销", required = true, allowableValues = {"1", "2", "3"})
    private Integer status;
    
    @Schema(description = "操作原因")
    private String reason;
}

