package com.wreckloud.wolfchat.account.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @Description 行者主页主题分页 VO
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@Schema(description = "行者主页主题分页")
public class UserHomeThreadPageVO {
    @Schema(description = "列表")
    private List<UserHomeThreadVO> list;

    @Schema(description = "总数")
    private Long total;

    @Schema(description = "页码")
    private Long page;

    @Schema(description = "每页数量")
    private Long size;
}
