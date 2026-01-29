package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * @Description 注册请求 DTO
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Data
@Schema(description = "注册请求")
public class RegisterDTO {
    @NotBlank(message = "行者名不能为空")
    @Size(min = 1, max = 64, message = "行者名长度必须在1-64个字符之间")
    @Schema(description = "行者名（行者在群落中的称呼，将被其他行者看到）", example = "行者名", required = true)
    private String nickname;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度必须在6-64个字符之间")
    @Schema(description = "登录密码（长度6-64位）", example = "123456", required = true)
    private String password;
}