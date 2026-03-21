package com.wreckloud.wolfchat.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 角色配置实体。
 */
@Data
@TableName("wf_ai_role")
public class WfAiRole {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色编码（唯一）。
     */
    private String roleCode;

    /**
     * 角色名称。
     */
    private String roleName;

    /**
     * 人设提示词。
     */
    private String personaPrompt;

    /**
     * 表达风格提示词。
     */
    private String stylePrompt;

    /**
     * 是否用于公共聊天室场景。
     */
    private Boolean sceneLobbyEnabled;

    /**
     * 是否用于私聊场景。
     */
    private Boolean scenePrivateEnabled;

    /**
     * 是否用于论坛场景。
     */
    private Boolean sceneForumEnabled;

    /**
     * 角色权重（越高越容易被选中）。
     */
    private Integer roleWeight;

    /**
     * 状态：NORMAL/DISABLED。
     */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

