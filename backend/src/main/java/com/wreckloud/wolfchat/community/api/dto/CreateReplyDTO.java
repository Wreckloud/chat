package com.wreckloud.wolfchat.community.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;

/**
 * @Description 发布回复 DTO
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@Schema(description = "发布回复请求")
public class CreateReplyDTO {
    @Size(max = 2000, message = "回复内容长度不能超过2000个字符")
    @Schema(description = "回复内容，可为空")
    private String content;

    @Schema(description = "引用楼层ID")
    private Long quoteReplyId;

    @Size(max = 255, message = "图片对象长度不能超过255个字符")
    @Schema(description = "回复图片对象 Key")
    private String imageKey;
}
