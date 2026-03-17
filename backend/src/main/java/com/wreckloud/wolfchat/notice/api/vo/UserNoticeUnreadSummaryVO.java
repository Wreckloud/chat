package com.wreckloud.wolfchat.notice.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description 用户通知未读分类统计 VO
 * @Author Wreckloud
 * @Date 2026-03-17
 */
@Data
@Schema(description = "用户通知未读分类统计")
public class UserNoticeUnreadSummaryVO {
    @Schema(description = "未读总数")
    private Long totalUnread;

    @Schema(description = "成就类未读数")
    private Long achievementUnread;

    @Schema(description = "关注类未读数")
    private Long followUnread;

    @Schema(description = "互动类未读数")
    private Long interactionUnread;
}
