package com.wreckloud.wolfchat.community.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 帖子信息 VO
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Data
@Schema(description = "帖子信息")
public class PostVO {
    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "帖子内容")
    private String content;

    @Schema(description = "关联聊天室ID")
    private Long roomId;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    @Schema(description = "发布者信息")
    private UserBriefVO author;
}
