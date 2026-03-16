package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Description 更新佩戴头衔 DTO
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@Schema(description = "更新佩戴头衔参数")
public class UpdateEquippedTitleDTO {
    @NotBlank
    @Schema(description = "成就编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "WOLF_CUB")
    private String achievementCode;
}
