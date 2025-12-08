package com.wreckloud.wolfchat.account.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description 登录响应VO（包含token和用户信息）
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Data
@Schema(description = "登录响应")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String token;

    @Schema(description = "Token类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "用户信息")
    private UserVO user;

    public LoginVO() {
    }

    public LoginVO(String token, UserVO user) {
        this.token = token;
        this.user = user;
    }
}

