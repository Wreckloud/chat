package com.wreckloud.wolfchat.common.excption;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Description 统一错误码枚举
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 通用错误码 1000-1999
    SUCCESS(0, "操作成功"),
    PARAM_ERROR(1001, "参数错误"),
    SYSTEM_ERROR(1002, "系统错误"),

    // 认证相关错误码 2000-2999
    UNAUTHORIZED(2001, "请先登录"),
    TOKEN_INVALID(2002, "token 无效"),
    TOKEN_EXPIRED(2003, "token 已过期"),
    LOGIN_FAILED(2004, "登录失败"),
    WOLF_NO_NOT_FOUND(2005, "狼藉号不存在"),
    LOGIN_KEY_ERROR(2006, "密码错误"),
    USER_DISABLED(2007, "行者账号已禁用"),

    // 业务错误码 3000-3999
    WOLF_NO_ALLOCATE_FAILED(3001, "狼藉号分配失败"),
    WOLF_NO_POOL_EMPTY(3002, "狼藉号池已空，请稍后重试"),
    USER_NOT_FOUND(3003, "行者不存在");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;
}

