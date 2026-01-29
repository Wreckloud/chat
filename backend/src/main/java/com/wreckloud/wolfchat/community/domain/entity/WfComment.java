package com.wreckloud.wolfchat.community.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.community.domain.enums.CommentStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 评论实体
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Data
@TableName("wf_comment")
public class WfComment {
    /**
     * 评论ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 评论者ID
     */
    private Long userId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 状态：NORMAL/DELETED
     */
    private CommentStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
