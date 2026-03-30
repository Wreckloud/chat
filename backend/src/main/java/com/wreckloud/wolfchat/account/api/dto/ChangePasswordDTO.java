package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * @Description 修改密码请求 DTO
 * @Author Wreckloud
 * @Date 2026-03-04
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordDTO {
    @NotBlank(message = "原密码不能为空")
    @Size(min = 6, max = 64, message = "原密码长度必须在6-64个字符之间")
    @Schema(description = "原密码（长度6-64位）", example = "123456", required = true)
    private String oldLoginKey;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码长度必须在6-64个字符之间")
    @Schema(description = "新密码（长度6-64位）", example = "12345678", required = true)
    private String newLoginKey;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 6, max = 64, message = "确认密码长度必须在6-64个字符之间")
    @Schema(description = "确认密码（长度6-64位）", example = "12345678", required = true)
    private String confirmLoginKey;

    @NotBlank(message = "邮箱验证码不能为空")
    @Size(min = 6, max = 6, message = "邮箱验证码必须为6位")
    @Schema(description = "改密邮箱验证码（6位）", example = "123456", required = true)
    private String emailCode;
}
