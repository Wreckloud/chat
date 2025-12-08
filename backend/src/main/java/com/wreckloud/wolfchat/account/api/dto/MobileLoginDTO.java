package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * @Description 手机号验证码登录请求DTO
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Data
@Schema(description = "手机号验证码登录请求")
public class MobileLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "手机号", required = true, example = "13800138000", pattern = "^1\\d{10}$")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;

    @Schema(description = "验证码key，从发送验证码接口返回", required = true, example = "13800138000:1234567890")
    @NotBlank(message = "验证码 key 不能为空")
    private String smsCodeKey;

    @Schema(description = "验证码，用户输入的6位数字验证码", required = true, example = "123456")
    @NotBlank(message = "验证码不能为空")
    private String smsCode;
}

