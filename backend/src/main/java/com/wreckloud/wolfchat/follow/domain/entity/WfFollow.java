package com.wreckloud.wolfchat.follow.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.follow.domain.enums.FollowStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 关注关系实体
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Data
@TableName("wf_follow")
public class WfFollow {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关注者ID
     */
    private Long followerId;

    /**
     * 被关注者ID
     */
    private Long followeeId;

    /**
     * 状态：FOLLOWING/UNFOLLOWED
     */
    private FollowStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
