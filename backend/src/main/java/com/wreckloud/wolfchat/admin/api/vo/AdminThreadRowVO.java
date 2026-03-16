package com.wreckloud.wolfchat.admin.api.vo;

import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端主题列表行。
 */
@Data
@Schema(description = "管理端主题列表行")
public class AdminThreadRowVO {
    @Schema(description = "主题ID")
    private Long threadId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "作者昵称")
    private String authorNickname;

    @Schema(description = "主题状态")
    private ForumThreadStatus status;

    @Schema(description = "主题类型")
    private ForumThreadType threadType;

    @Schema(description = "是否精华")
    private Boolean isEssence;

    @Schema(description = "回复数")
    private Integer replyCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

