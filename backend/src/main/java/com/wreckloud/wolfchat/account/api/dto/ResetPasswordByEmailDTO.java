package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * @Description 通过邮箱验证码重置密码请求 DTO
 * @Author Wreckloud
 * @Date 2026-03-04
 */
@Data
@Schema(description = "通过邮箱验证码重置密码请求")
public class ResetPasswordByEmailDTO {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "user@example.com", required = true)
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
    @Schema(description = "邮箱验证码（6位数字）", example = "123456", required = true)
    private String verifyCode;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码长度必须在6-64个字符之间")
    @Schema(description = "新密码（长度6-64位）", example = "12345678", required = true)
    private String newLoginKey;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 6, max = 64, message = "确认密码长度必须在6-64个字符之间")
    @Schema(description = "确认密码（长度6-64位）", example = "12345678", required = true)
    private String confirmLoginKey;
}
