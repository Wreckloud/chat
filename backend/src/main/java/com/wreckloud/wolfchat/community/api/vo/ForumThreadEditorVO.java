package com.wreckloud.wolfchat.community.api.vo;

import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 主题编辑器数据 VO
 */
@Data
@Schema(description = "主题编辑器数据")
public class ForumThreadEditorVO {
    @Schema(description = "主题ID")
    private Long threadId;

    @Schema(description = "主题状态")
    private ForumThreadStatus status;

    @Schema(description = "主题标题")
    private String title;

    @Schema(description = "主题正文")
    private String content;

    @Schema(description = "图片对象 Key 列表")
    private List<String> imageKeys;

    @Schema(description = "图片 URL 列表")
    private List<String> imageUrls;

    @Schema(description = "视频对象 Key")
    private String videoKey;

    @Schema(description = "视频 URL")
    private String videoUrl;

    @Schema(description = "视频封面对象 Key")
    private String videoPosterKey;

    @Schema(description = "视频封面 URL")
    private String videoPosterUrl;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "编辑时间")
    private LocalDateTime editTime;
}

