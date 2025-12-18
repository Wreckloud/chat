package com.wreckloud.wolfchat.group.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @Description 转让群主DTO
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Data
@Schema(description = "转让群主请求参数")
public class GroupTransferDTO {

    @Schema(description = "新群主用户ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "新群主用户ID不能为空")
    private Long newOwnerId;
}

