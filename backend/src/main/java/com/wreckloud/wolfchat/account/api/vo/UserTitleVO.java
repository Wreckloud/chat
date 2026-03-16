package com.wreckloud.wolfchat.account.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description 用户头衔 VO
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@Schema(description = "用户头衔")
public class UserTitleVO {
    @Schema(description = "成就编码")
    private String achievementCode;

    @Schema(description = "头衔名称")
    private String titleName;

    @Schema(description = "头衔颜色")
    private String titleColor;
}
