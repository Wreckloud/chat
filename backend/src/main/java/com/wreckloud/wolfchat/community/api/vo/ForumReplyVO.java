package com.wreckloud.wolfchat.community.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 论坛回复信息 VO
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@Schema(description = "论坛回复信息")
public class ForumReplyVO {
    @Schema(description = "回复ID")
    private Long replyId;

    @Schema(description = "主题ID")
    private Long threadId;

    @Schema(description = "楼层号")
    private Integer floorNo;

    @Schema(description = "回复内容")
    private String content;

    @Schema(description = "引用回复ID")
    private Long quoteReplyId;

    @Schema(description = "引用楼层号")
    private Integer quoteFloorNo;

    @Schema(description = "引用作者")
    private UserBriefVO quoteAuthor;

    @Schema(description = "引用内容")
    private String quoteContent;

    @Schema(description = "回复时间")
    private LocalDateTime createTime;

    @Schema(description = "回复者")
    private UserBriefVO author;
}
