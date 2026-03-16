package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 管理端分页响应。
 */
@Data
@Schema(description = "管理端分页响应")
public class AdminPageVO<T> {
    @Schema(description = "当前页码")
    private long page;

    @Schema(description = "每页大小")
    private long size;

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "列表数据")
    private List<T> list;

    public static <T> AdminPageVO<T> of(long page, long size, long total, List<T> list) {
        AdminPageVO<T> vo = new AdminPageVO<>();
        vo.setPage(page);
        vo.setSize(size);
        vo.setTotal(total);
        vo.setList(list == null ? Collections.emptyList() : list);
        return vo;
    }
}

