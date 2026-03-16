package com.wreckloud.wolfchat.account.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Description 行者主页 VO
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@Schema(description = "行者主页")
public class UserHomeVO {
    @Schema(description = "用户公开信息")
    private UserPublicVO user;

    @Schema(description = "是否本人")
    private Boolean self;

    @Schema(description = "是否已关注对方")
    private Boolean following;

    @Schema(description = "是否互关")
    private Boolean mutual;

    @Schema(description = "活跃天数")
    private Integer activeDayCount;

    @Schema(description = "总获赞")
    private Integer totalLikeCount;

    @Schema(description = "粉丝数")
    private Integer followerCount;

    @Schema(description = "关注数")
    private Integer followingCount;

    @Schema(description = "发帖数")
    private Integer threadCount;

    @Schema(description = "回帖数")
    private Integer replyCount;

    @Schema(description = "最近活跃时间")
    private LocalDateTime lastActiveAt;

    @Schema(description = "头衔橱窗（最多3个）")
    private List<UserTitleVO> showcaseTitles;

    @Schema(description = "最近发布主题（默认最多3条）")
    private List<UserHomeThreadVO> latestThreads;
}
