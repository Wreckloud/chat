package com.wreckloud.wolfchat.account.api.dto;

import com.wreckloud.wolfchat.account.domain.enums.OnboardingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @Description 更新新用户引导状态请求 DTO
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Data
@Schema(description = "更新新用户引导状态请求")
public class UpdateOnboardingStatusDTO {
    @NotNull(message = "引导状态不能为空")
    @Schema(description = "引导状态：PENDING/COMPLETED/SKIPPED", required = true)
    private OnboardingStatus onboardingStatus;
}
