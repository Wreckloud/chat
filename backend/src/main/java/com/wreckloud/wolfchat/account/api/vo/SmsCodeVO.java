package com.wreckloud.wolfchat.account.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Description 短信验证码响应VO
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "短信验证码响应")
public class SmsCodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "验证码key（用于后续验证）", example = "13800138000:1234567890")
    private String smsCodeKey;

    @Schema(description = "提示信息（开发环境会返回验证码，生产环境不返回）", example = "验证码已发送，请查收短信")
    private String message;
}

