package com.wreckloud.wolfchat.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Description 管理员查看用户信息VO
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员查看用户信息")
public class AdminUserVO {
    
    @Schema(description = "用户ID")
    private Long id;
    
    @Schema(description = "WF号")
    private Long wfNo;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "手机号")
    private String mobile;
    
    @Schema(description = "邮箱")
    private String email;
    
    @Schema(description = "头像URL")
    private String avatar;
    
    @Schema(description = "性别：0未知 1男 2女")
    private Integer gender;
    
    @Schema(description = "个性签名")
    private String signature;
    
    @Schema(description = "状态：1正常 2禁用 3注销")
    private Integer status;
    
    @Schema(description = "角色：1普通用户 2管理员")
    private Integer role;
    
    @Schema(description = "最后登录IP")
    private String lastLoginIp;
    
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;
    
    @Schema(description = "最后下线时间")
    private LocalDateTime lastLogoutTime;
    
    @Schema(description = "注册时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}

