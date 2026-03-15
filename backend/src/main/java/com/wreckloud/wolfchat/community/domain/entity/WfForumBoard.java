package com.wreckloud.wolfchat.community.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.community.domain.enums.ForumBoardStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 论坛版块实体
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Data
@TableName("wf_forum_board")
public class WfForumBoard {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 版块名称
     */
    private String name;

    /**
     * 版块短标识
     */
    private String slug;

    /**
     * 版块描述
     */
    private String description;

    /**
     * 排序号（越小越靠前）
     */
    private Integer sortNo;

    /**
     * 状态：NORMAL/CLOSED
     */
    private ForumBoardStatus status;

    /**
     * 主题数
     */
    private Integer threadCount;

    /**
     * 回复数
     */
    private Integer replyCount;

    /**
     * 最近活跃主题ID
     */
    private Long lastThreadId;

    /**
     * 最近活跃时间
     */
    private LocalDateTime lastReplyTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
