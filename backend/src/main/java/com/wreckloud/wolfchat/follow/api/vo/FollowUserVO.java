package com.wreckloud.wolfchat.follow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description 关注列表行者信息 VO
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Data
@Schema(description = "关注行者信息")
public class FollowUserVO {
    @Schema(description = "行者ID")
    private Long userId;

    @Schema(description = "狼藉号")
    private String wolfNo;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "状态：NORMAL-正常，DISABLED-禁用")
    private String status;

    @Schema(description = "是否互关")
    private Boolean mutual;
}
