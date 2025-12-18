package com.wreckloud.wolfchat.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @Description 管理员解散群组DTO
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Data
@Schema(description = "解散群组请求")
public class GroupDismissDTO {
    
    @NotNull(message = "群组ID不能为空")
    @Schema(description = "群组ID", required = true)
    private Long groupId;
    
    @Schema(description = "解散原因")
    private String reason;
}

