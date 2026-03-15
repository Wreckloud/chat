package com.wreckloud.wolfchat.community.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @Description 论坛主题分页 VO
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@Schema(description = "论坛主题分页结果")
public class ForumThreadPageVO {
    @Schema(description = "主题列表")
    private List<ForumThreadVO> list;

    @Schema(description = "总数")
    private Long total;

    @Schema(description = "页码")
    private Long page;

    @Schema(description = "每页数量")
    private Long size;
}
