package com.wreckloud.wolfchat.community.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 评论信息 VO
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Data
@Schema(description = "评论信息")
public class CommentVO {
    @Schema(description = "评论ID")
    private Long commentId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评论时间")
    private LocalDateTime createTime;

    @Schema(description = "评论者信息")
    private UserBriefVO author;
}
