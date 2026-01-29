package com.wreckloud.wolfchat.account.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description 登录响应 VO
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Data
@Schema(description = "登录响应")
public class LoginVO {
    @Schema(description = "JWT token")
    private String token;

    @Schema(description = "行者信息")
    private UserVO userInfo;
}

