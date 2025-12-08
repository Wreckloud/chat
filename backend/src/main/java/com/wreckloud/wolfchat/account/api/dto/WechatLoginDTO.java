package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @Description 微信登录请求DTO
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Data
@Schema(description = "微信登录请求")
public class WechatLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "微信openid（单应用唯一）", required = true, example = "oUpF8uMuAJO_M2pxb1Q9zNjWeS6o")
    @NotBlank(message = "openid不能为空")
    private String wxOpenid;

    @Schema(description = "微信unionid（跨应用唯一，可选）", example = "o6_bmjrPTlm6_2sgVt7hMZOPfL2M")
    private String wxUnionid;

    @Schema(description = "微信昵称（可选）", example = "微信用户")
    private String nickname;

    @Schema(description = "微信头像URL（可选）", example = "https://thirdwx.qlogo.cn/...")
    private String avatar;

    @Schema(description = "性别：0未知 1男 2女（可选）", example = "1")
    private Integer gender;
}

