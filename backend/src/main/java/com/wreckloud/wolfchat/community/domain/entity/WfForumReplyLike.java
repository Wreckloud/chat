package com.wreckloud.wolfchat.community.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 回复点赞关系实体
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@TableName("wf_forum_reply_like")
public class WfForumReplyLike {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 回复ID
     */
    private Long replyId;

    /**
     * 点赞用户ID
     */
    private Long userId;

    private LocalDateTime createTime;
}
