package com.wreckloud.wolfchat.community.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.community.domain.enums.ForumReplyStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 论坛回复实体
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@TableName("wf_forum_reply")
public class WfForumReply {
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
     * 楼层号（首帖为1楼，回复从2楼开始）
     */
    private Integer floorNo;

    /**
     * 作者ID
     */
    private Long authorId;

    /**
     * 回复内容
     */
    private String content;

    /**
     * 回复图片对象 Key
     */
    private String imageKey;

    /**
     * 引用楼层ID
     */
    private Long quoteReplyId;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 状态：NORMAL/DELETED
     */
    private ForumReplyStatus status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
