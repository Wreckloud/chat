package com.wreckloud.wolfchat.community.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 主题点赞关系实体
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@TableName("wf_forum_thread_like")
public class WfForumThreadLike {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 主题ID
     */
    private Long threadId;

    /**
     * 点赞用户ID
     */
    private Long userId;

    private LocalDateTime createTime;
}
