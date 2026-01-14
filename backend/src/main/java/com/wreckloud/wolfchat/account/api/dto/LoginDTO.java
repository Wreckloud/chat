package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Description 登录请求 DTO
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Data
@Schema(description = "登录请求")
public class LoginDTO {
    @NotBlank(message = "狼藉号不能为空")
    @Schema(description = "狼藉号", example = "1234567890", required = true)
    private String wolfNo;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "登录密码", example = "123456", required = true)
    private String loginKey;
}

