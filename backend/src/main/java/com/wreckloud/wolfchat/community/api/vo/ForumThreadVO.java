package com.wreckloud.wolfchat.community.api.vo;

import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Description 论坛主题信息 VO
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@Schema(description = "论坛主题信息")
public class ForumThreadVO {
    @Schema(description = "主题ID")
    private Long threadId;

    @Schema(description = "版块ID")
    private Long boardId;

    @Schema(description = "版块名称")
    private String boardName;

    @Schema(description = "主题标题")
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

    @Schema(description = "当前登录用户是否已点赞")
    private Boolean likedByCurrentUser;

    @Schema(description = "首帖图片 URL 列表")
    private List<String> imageUrls;

    @Schema(description = "首帖视频 URL")
    private String videoUrl;

    @Schema(description = "首帖视频封面 URL")
    private String videoPosterUrl;

    @Schema(description = "正文摘要")
    private String contentPreview;

    @Schema(description = "最近回复时间")
    private LocalDateTime lastReplyTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "作者")
    private UserBriefVO author;

    @Schema(description = "最后回复者")
    private UserBriefVO lastReplyUser;
}
