package com.wreckloud.wolfchat.account.api.vo;

import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description 行者公开信息响应 VO
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Data
@Schema(description = "行者公开信息")
public class UserPublicVO {
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "狼藉号", example = "1234567890")
    private String wolfNo;

    @Schema(description = "行者名（用户昵称）", example = "维克罗德")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "个性签名")
    private String signature;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "当前佩戴头衔名称")
    private String equippedTitleName;

    @Schema(description = "当前佩戴头衔颜色")
    private String equippedTitleColor;

    @Schema(description = "状态：NORMAL-正常，DISABLED-禁用或已注销")
    private UserStatus status;
}
