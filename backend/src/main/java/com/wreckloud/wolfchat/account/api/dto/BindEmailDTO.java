package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * @Description 绑定邮箱请求 DTO
 * @Author Wreckloud
 * @Date 2026-03-04
 */
@Data
@Schema(description = "绑定邮箱请求")
public class BindEmailDTO {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "待绑定邮箱", example = "user@example.com", required = true)
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
    @Schema(description = "邮箱验证码（6位数字）", example = "123456", required = true)
    private String verifyCode;
}
