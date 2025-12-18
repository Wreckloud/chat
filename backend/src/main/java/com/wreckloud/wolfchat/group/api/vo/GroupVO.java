package com.wreckloud.wolfchat.group.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 群组基础信息VO
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Data
@Schema(description = "群组基础信息")
public class GroupVO {

    @Schema(description = "群组ID", example = "1")
    private Long groupId;

    @Schema(description = "群名称", example = "2024级软件工程1班")
    private String groupName;

    @Schema(description = "群头像URL", example = "https://example.com/avatar.jpg")
    private String groupAvatar;

    @Schema(description = "群简介", example = "我们是最棒的班级")
    private String groupIntro;

    @Schema(description = "群主用户ID", example = "1")
    private Long ownerId;

    @Schema(description = "群主WF号", example = "10001")
    private Long ownerWfNo;

    @Schema(description = "群主昵称", example = "张三")
    private String ownerName;

    @Schema(description = "当前成员数", example = "50")
    private Integer memberCount;

    @Schema(description = "最大成员数", example = "200")
    private Integer maxMembers;

    @Schema(description = "我在群中的角色：OWNER-群主 ADMIN-管理员 MEMBER-成员", example = "MEMBER")
    private String myRole;

    @Schema(description = "创建时间", example = "2024-12-18 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

