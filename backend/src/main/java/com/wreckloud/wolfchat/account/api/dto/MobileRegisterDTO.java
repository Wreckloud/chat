package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;

/**
 * @Description 手机号注册请求DTO
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Data
@Schema(description = "手机号注册请求")
public class MobileRegisterDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "手机号", required = true, example = "13800138000", pattern = "^1\\d{10}$")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;

    @Schema(description = "密码，长度6-32位", required = true, example = "123456", minLength = 6, maxLength = 32)
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需要在 6-32 位之间")
    private String password;

    @Schema(description = "确认密码，用于前端二次确认（可选）", example = "123456")
    private String confirmPassword;

    @Schema(description = "验证码key，从获取验证码接口返回", required = true, example = "abc123def456")
    @NotBlank(message = "验证码 key 不能为空")
    private String captchaKey;

    @Schema(description = "验证码，用户输入的验证码", required = true, example = "ABCD")
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
