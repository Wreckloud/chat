package com.wreckloud.wolfchat.account.api.vo;

import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Description 行者主页主题摘要 VO
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@Schema(description = "行者主页主题摘要")
public class UserHomeThreadVO {
    @Schema(description = "主题ID")
    private Long threadId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "主题类型")
    private ForumThreadType threadType;

    @Schema(description = "主题状态")
    private ForumThreadStatus status;

    @Schema(description = "是否精华")
    private Boolean isEssence;

    @Schema(description = "浏览数")
    private Integer viewCount;

    @Schema(description = "回复数")
    private Integer replyCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "正文摘要")
    private String contentPreview;

    @Schema(description = "图片 URL 列表")
    private List<String> imageUrls;

    @Schema(description = "视频封面 URL")
    private String videoPosterUrl;

    @Schema(description = "最后回复时间")
    private LocalDateTime lastReplyTime;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    @Schema(description = "编辑时间（未编辑为空）")
    private LocalDateTime editTime;
}
