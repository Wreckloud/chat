package com.wreckloud.wolfchat.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 管理端登录请求。
 */
@Data
@Schema(description = "管理端登录请求")
public class AdminLoginDTO {
    @NotBlank(message = "账号不能为空")
    @Schema(description = "登录账号（狼藉号或邮箱）", required = true)
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度必须在6-64个字符之间")
    @Schema(description = "登录密码", required = true)
    private String password;
}

