package com.wreckloud.wolfchat.account.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 用户成就信息 VO
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@Schema(description = "用户成就")
public class UserAchievementVO {
    @Schema(description = "成就编码")
    private String achievementCode;

    @Schema(description = "成就名称")
    private String name;

    @Schema(description = "成就描述")
    private String description;

    @Schema(description = "头衔名称")
    private String titleName;

    @Schema(description = "头衔颜色")
    private String titleColor;

    @Schema(description = "是否已解锁")
    private Boolean unlocked;

    @Schema(description = "是否当前佩戴")
    private Boolean equipped;

    @Schema(description = "解锁时间")
    private LocalDateTime unlockTime;
}
