package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Description 管理员查看群组信息VO
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员查看群组信息")
public class AdminGroupVO {
    
    @Schema(description = "群组ID")
    private Long id;
    
    @Schema(description = "群名称")
    private String groupName;
    
    @Schema(description = "群头像")
    private String groupAvatar;
    
    @Schema(description = "群简介")
    private String groupIntro;
    
    @Schema(description = "群主用户ID")
    private Long ownerId;
    
    @Schema(description = "群主用户名")
    private String ownerName;
    
    @Schema(description = "群主WF号")
    private Long ownerWfNo;
    
    @Schema(description = "成员数量")
    private Integer memberCount;
    
    @Schema(description = "最大成员数量")
    private Integer maxMembers;
    
    @Schema(description = "是否全员禁言")
    private Boolean isAllMuted;
    
    @Schema(description = "是否需要审批")
    private Boolean isNeedApproval;
    
    @Schema(description = "群状态：1正常 2已解散")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}

