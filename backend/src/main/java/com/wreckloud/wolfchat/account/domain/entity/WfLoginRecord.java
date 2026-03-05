package com.wreckloud.wolfchat.account.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wreckloud.wolfchat.account.domain.enums.LoginMethod;
import com.wreckloud.wolfchat.account.domain.enums.LoginResult;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description 登录记录实体
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Data
@TableName("wf_login_record")
public class WfLoginRecord {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（未匹配到用户时为空）
     */
    private Long userId;

    /**
     * 登录方式：WOLF_NO/EMAIL/UNKNOWN
     */
    private LoginMethod loginMethod;

    /**
     * 登录结果：SUCCESS/FAIL
     */
    private LoginResult loginResult;

    /**
     * 失败错误码（登录成功时为空）
     */
    private Integer failCode;

    /**
     * 脱敏后的登录账号
     */
    private String accountMask;

    /**
     * 客户端IP
     */
    private String ip;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 客户端类型（例如 WECHAT_MINIPROGRAM）
     */
    private String clientType;

    /**
     * 客户端版本
     */
    private String clientVersion;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;
}
