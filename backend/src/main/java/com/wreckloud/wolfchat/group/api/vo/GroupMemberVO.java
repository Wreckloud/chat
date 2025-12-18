package com.wreckloud.wolfchat.group.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 群成员信息VO
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Data
@Schema(description = "群成员信息")
public class GroupMemberVO {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "WF号", example = "10001")
    private Long wfNo;

    @Schema(description = "用户昵称", example = "张三")
    private String username;

    @Schema(description = "用户头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "群昵称（群名片）", example = "班长")
    private String groupNickname;

    @Schema(description = "角色：OWNER-群主 ADMIN-管理员 MEMBER-成员", example = "MEMBER")
    private String role;

    @Schema(description = "是否被禁言", example = "false")
    private Boolean isMuted;

    @Schema(description = "加入时间", example = "2024-12-18 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinTime;
}

