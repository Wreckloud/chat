package com.wreckloud.wolfchat.community.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @Description 帖子分页 VO
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Data
@Schema(description = "帖子分页结果")
public class PostPageVO {
    @Schema(description = "帖子列表")
    private List<PostVO> list;

    @Schema(description = "总数")
    private Long total;

    @Schema(description = "页码")
    private Long page;

    @Schema(description = "每页数量")
    private Long size;
}
