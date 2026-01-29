package com.wreckloud.wolfchat.community.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @Description 帖子详情 VO
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Data
@Schema(description = "帖子详情")
public class PostDetailVO {
    @Schema(description = "帖子信息")
    private PostVO post;

    @Schema(description = "评论列表")
    private List<CommentVO> comments;
}
