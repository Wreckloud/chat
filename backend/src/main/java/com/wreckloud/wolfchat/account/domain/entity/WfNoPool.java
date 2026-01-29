package com.wreckloud.wolfchat.account.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.account.domain.enums.NoPoolStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 狼藉号池实体
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Data
@TableName("wf_no_pool")
public class WfNoPool {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 狼藉号（10位，首位1-9，避免前导0）
     */
    private String wolfNo;

    /**
     * 状态：UNUSED-未使用，USED-已使用，RESERVED-预留
     */
    private NoPoolStatus status;

    /**
     * 已使用时绑定的行者ID
     */
    private Long userId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}


