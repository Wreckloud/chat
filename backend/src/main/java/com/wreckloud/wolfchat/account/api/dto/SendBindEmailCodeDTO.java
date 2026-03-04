package com.wreckloud.wolfchat.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * @Description 发送绑定邮箱验证码请求 DTO
 * @Author Wreckloud
 * @Date 2026-03-04
 */
@Data
@Schema(description = "发送绑定邮箱验证码请求")
public class SendBindEmailCodeDTO {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "待绑定邮箱", example = "user@example.com", required = true)
    private String email;
}
