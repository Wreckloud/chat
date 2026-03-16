package com.wreckloud.wolfchat.notice.api.vo;

import com.wreckloud.wolfchat.notice.domain.enums.NoticeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 用户通知 VO
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@Schema(description = "用户通知")
public class UserNoticeVO {
    @Schema(description = "通知ID")
    private Long noticeId;

    @Schema(description = "通知类型")
    private NoticeType noticeType;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务ID")
    private Long bizId;

    @Schema(description = "是否已读")
    private Boolean read;

    @Schema(description = "已读时间")
    private LocalDateTime readTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
