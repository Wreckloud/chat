package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端回复列表行。
 */
@Data
@Schema(description = "管理端回复列表行")
public class AdminReplyRowVO {
    @Schema(description = "回复ID")
    private Long replyId;

    @Schema(description = "主题ID")
    private Long threadId;

    @Schema(description = "作者昵称")
    private String authorNickname;

    @Schema(description = "回复内容")
    private String content;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

