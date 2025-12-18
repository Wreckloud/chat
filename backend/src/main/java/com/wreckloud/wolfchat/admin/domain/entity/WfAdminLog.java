package com.wreckloud.wolfchat.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Description 管理员操作日志实体类
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_admin_log")
public class WfAdminLog implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 管理员用户ID
     */
    private Long adminId;
    
    /**
     * 管理员用户名
     */
    private String adminName;
    
    /**
     * 管理员WF号
     */
    private Long adminWfNo;
    
    /**
     * 操作类型：disable_user/enable_user/dismiss_group等
     */
    private String action;
    
    /**
     * 目标类型：user/group/system
     */
    private String targetType;
    
    /**
     * 目标ID
     */
    private Long targetId;
    
    /**
     * 目标名称
     */
    private String targetName;
    
    /**
     * 操作详情JSON
     */
    private String details;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 操作结果：1成功 0失败
     */
    private Integer result;
    
    /**
     * 错误信息（失败时）
     */
    private String errorMessage;
    
    /**
     * 操作时间
     */
    private LocalDateTime createTime;
}

