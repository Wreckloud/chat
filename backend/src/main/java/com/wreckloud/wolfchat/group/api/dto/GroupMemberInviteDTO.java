package com.wreckloud.wolfchat.group.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @Description 邀请成员入群DTO
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Data
@Schema(description = "邀请成员入群请求参数")
public class GroupMemberInviteDTO {

    @Schema(description = "被邀请用户ID列表", example = "[2, 3, 4]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "成员列表不能为空")
    private List<Long> userIds;
}

