package com.wreckloud.wolfchat.community.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description 行者简要信息 VO
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Data
@Schema(description = "行者简要信息")
public class UserBriefVO {
    @Schema(description = "行者ID")
    private Long userId;

    @Schema(description = "狼藉号")
    private String wolfNo;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;
}
