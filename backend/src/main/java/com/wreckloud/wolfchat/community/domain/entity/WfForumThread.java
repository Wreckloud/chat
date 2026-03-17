package com.wreckloud.wolfchat.community.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 论坛主题实体
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@TableName("wf_forum_thread")
public class WfForumThread {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 版块ID
     */
    private Long boardId;

    /**
     * 作者ID
     */
    private Long authorId;

    /**
     * 标题
     */
    private String title;

    /**
     * 首帖内容
     */
    private String content;

    /**
     * 首帖图片对象 Key 列表（英文逗号分隔）
     */
    private String imageKeys;

    /**
     * 首帖视频对象 Key
     */
    private String videoKey;

    /**
     * 主题类型：NORMAL/STICKY/ANNOUNCEMENT
     */
    private ForumThreadType threadType;

    /**
     * 状态：NORMAL/LOCKED/DELETED
     */
    private ForumThreadStatus status;

    /**
     * 是否精华
     */
    private Boolean isEssence;

    /**
     * 浏览数
     */
    private Integer viewCount;

    /**
     * 回复数
     */
    private Integer replyCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 最后一条回复ID
     */
    private Long lastReplyId;

    /**
     * 最后回复者ID
     */
    private Long lastReplyUserId;

    /**
     * 最后回复时间
     */
    private LocalDateTime lastReplyTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
