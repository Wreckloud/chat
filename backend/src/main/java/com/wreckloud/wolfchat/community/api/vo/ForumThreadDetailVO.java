package com.wreckloud.wolfchat.community.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description 论坛主题详情 VO
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@Schema(description = "论坛主题详情")
public class ForumThreadDetailVO {
    @Schema(description = "主题信息")
    private ForumThreadVO thread;

    @Schema(description = "首帖正文")
    private String content;
}
