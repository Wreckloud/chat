package com.wreckloud.wolfchat.account.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 用户成就实体
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@TableName("wf_user_achievement")
public class WfUserAchievement {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String achievementCode;

    private LocalDateTime unlockTime;

    private LocalDateTime createTime;
}
