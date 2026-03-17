package com.wreckloud.wolfchat.notice.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.notice.domain.enums.NoticeType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 用户通知实体
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@TableName("wf_user_notice")
public class WfUserNotice {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private NoticeType noticeType;

    private String content;

    private String bizType;

    private Long bizId;

    @TableField("is_read")
    private Boolean readFlag;

    private LocalDateTime readTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
