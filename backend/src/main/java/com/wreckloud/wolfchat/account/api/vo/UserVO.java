package com.wreckloud.wolfchat.account.api.vo;

import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description 行者信息响应 VO
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Data
@Schema(description = "行者信息")
public class UserVO {
    @Schema(description = "狼藉号", example = "1234567890")
    private String wolfNo;

    @Schema(description = "行者名（用户昵称）", example = "维克罗德")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "状态：NORMAL-正常，DISABLED-禁用")
    private UserStatus status;
}


