package com.wreckloud.wolfchat.community.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * @Description 发布回复 DTO
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@Schema(description = "发布回复请求")
public class CreateReplyDTO {
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 2000, message = "回复内容长度不能超过2000个字符")
    @Schema(description = "回复内容")
    private String content;

    @Schema(description = "引用楼层ID")
    private Long quoteReplyId;
}
