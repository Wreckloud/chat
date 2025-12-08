package com.wreckloud.wolfchat.account.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Description 验证码响应VO
 * @Author Wreckloud
 * @Date 2025-12-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证码响应")
public class CaptchaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "验证码key，用于后续验证，有效期5分钟", example = "abc123def456")
    private String captchaKey;

    @Schema(description = "验证码图片（Base64编码），可直接用于img标签的src属性", example = "data:image/png;base64,iVBORw0KGgo...")
    private String captchaImage;
}

