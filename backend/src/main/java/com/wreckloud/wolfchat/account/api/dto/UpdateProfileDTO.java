package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * @Description 更新个人资料请求 DTO
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@Schema(description = "更新个人资料请求")
public class UpdateProfileDTO {
    @NotBlank(message = "称谓不能为空")
    @Size(max = 64, message = "称谓长度不能超过64个字符")
    @Schema(description = "称谓", example = "维克罗德", required = true)
    private String nickname;

    @Size(max = 255, message = "签名长度不能超过255个字符")
    @Schema(description = "个性签名", example = "愿你被世界温柔以待")
    private String signature;

    @Size(max = 1000, message = "个人简介长度不能超过1000个字符")
    @Schema(description = "个人简介", example = "热爱复古界面与社区产品。")
    private String bio;
}
