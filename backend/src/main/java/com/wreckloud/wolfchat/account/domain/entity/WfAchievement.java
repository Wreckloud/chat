package com.wreckloud.wolfchat.account.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 成就定义实体
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Data
@TableName("wf_achievement")
public class WfAchievement {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String description;

    private String titleName;

    private String titleColor;

    private Integer sortNo;

    private Boolean enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
