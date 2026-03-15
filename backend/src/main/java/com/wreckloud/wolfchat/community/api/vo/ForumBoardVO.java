package com.wreckloud.wolfchat.community.api.vo;

import com.wreckloud.wolfchat.community.domain.enums.ForumBoardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 论坛版块信息 VO
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@Schema(description = "论坛版块信息")
public class ForumBoardVO {
    @Schema(description = "版块ID")
    private Long boardId;

    @Schema(description = "版块名称")
    private String name;

    @Schema(description = "版块短标识")
    private String slug;

    @Schema(description = "版块描述")
    private String description;

    @Schema(description = "排序号")
    private Integer sortNo;

    @Schema(description = "版块状态")
    private ForumBoardStatus status;

    @Schema(description = "主题数")
    private Integer threadCount;

    @Schema(description = "回复数")
    private Integer replyCount;

    @Schema(description = "最近活跃时间")
    private LocalDateTime lastReplyTime;
}
