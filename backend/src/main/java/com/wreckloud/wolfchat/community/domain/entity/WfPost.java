package com.wreckloud.wolfchat.community.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.community.domain.enums.PostStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 帖子实体
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Data
@TableName("wf_post")
public class WfPost {
    /**
     * 帖子ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发布者ID
     */
    private Long userId;

    /**
     * 帖子内容
     */
    private String content;

    /**
     * 关联聊天室ID（可选）
     */
    private Long roomId;

    /**
     * 状态：NORMAL/DELETED
     */
    private PostStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
