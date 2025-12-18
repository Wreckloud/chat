package com.wreckloud.wolfchat.group.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;

/**
 * @Description 修改群信息DTO
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Data
@Schema(description = "修改群信息请求参数")
public class GroupUpdateDTO {

    @Schema(description = "群名称", example = "新的群名称")
    @Size(max = 50, message = "群名称不能超过50个字符")
    private String groupName;

    @Schema(description = "群头像URL", example = "https://example.com/new-avatar.jpg")
    @Size(max = 255, message = "群头像URL不能超过255个字符")
    private String groupAvatar;

    @Schema(description = "群简介", example = "新的群简介")
    @Size(max = 500, message = "群简介不能超过500个字符")
    private String groupIntro;
}

