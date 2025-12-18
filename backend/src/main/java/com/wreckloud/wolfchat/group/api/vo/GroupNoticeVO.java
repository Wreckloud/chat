package com.wreckloud.wolfchat.group.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 群公告VO
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Data
@Schema(description = "群公告信息")
public class GroupNoticeVO {

    @Schema(description = "公告ID", example = "1")
    private Long noticeId;

    @Schema(description = "群组ID", example = "1")
    private Long groupId;

    @Schema(description = "公告标题", example = "班级通知")
    private String title;

    @Schema(description = "公告内容", example = "下周一上午9点开班会，请准时参加")
    private String content;

    @Schema(description = "发布者用户ID", example = "1")
    private Long publisherId;

    @Schema(description = "发布者昵称", example = "张三")
    private String publisherName;

    @Schema(description = "是否置顶", example = "true")
    private Boolean isPinned;

    @Schema(description = "发布时间", example = "2024-12-18 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
}

