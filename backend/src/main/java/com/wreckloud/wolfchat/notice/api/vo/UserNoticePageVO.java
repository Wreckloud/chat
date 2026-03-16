package com.wreckloud.wolfchat.notice.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @Description 用户通知分页 VO
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@Schema(description = "用户通知分页结果")
public class UserNoticePageVO {
    @Schema(description = "列表")
    private List<UserNoticeVO> list;

    @Schema(description = "总数")
    private Long total;

    @Schema(description = "页码")
    private Long page;

    @Schema(description = "每页数量")
    private Long size;
}
