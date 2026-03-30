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
    SYSTEM_ERROR(1002, "系统错误，请稍后重试"),
    DATABASE_ERROR(1003, "数据库操作失败，请稍后重试"),

    // 认证相关错误码 2000-2999
    UNAUTHORIZED(2001, "请先登录"),
    TOKEN_INVALID(2002, "token 无效"),
    LOGIN_FAILED(2004, "账号或密码错误"),
    WOLF_NO_NOT_FOUND(2005, "狼藉号不存在"),
    LOGIN_KEY_ERROR(2006, "密码错误"),
    USER_DISABLED(2007, "行者账号已禁用"),
    OLD_LOGIN_KEY_ERROR(2008, "原密码错误"),
    NEW_LOGIN_KEY_NOT_MATCH(2009, "新密码与确认密码不一致"),
    EMAIL_ALREADY_USED(2010, "邮箱已被占用"),
    EMAIL_NOT_BOUND(2011, "邮箱未绑定"),
    EMAIL_NOT_VERIFIED(2012, "邮箱未认证"),
    EMAIL_SEND_FAILED(2014, "邮件发送失败，请稍后重试"),
    EMAIL_SEND_TOO_FREQUENT(2015, "邮件发送过于频繁，请稍后重试"),
    EMAIL_DAILY_LIMIT(2016, "邮件发送次数已达上限"),
    EMAIL_VERIFY_LINK_INVALID(2017, "认证链接无效或已失效"),
    EMAIL_REBIND_NOT_ALLOWED(2018, "邮箱已绑定，不支持换绑"),
    PASSWORD_RESET_LINK_INVALID(2019, "重置密码链接无效或已失效"),
    ADMIN_FORBIDDEN(2020, "无管理端访问权限"),
    LOGIN_TOO_MANY_ATTEMPTS(2021, "登录尝试过于频繁，请稍后再试"),
    PASSWORD_CHANGE_CODE_INVALID(2022, "改密验证码无效或已过期"),
    PASSWORD_CHANGE_CODE_LOCKED(2023, "验证码错误次数过多，请重新发送"),

    // 业务错误码 3000-3999
    WOLF_NO_ALLOCATE_FAILED(3001, "狼藉号分配失败"),
    WOLF_NO_POOL_EMPTY(3002, "狼藉号池已空，请稍后重试"),
    USER_NOT_FOUND(3003, "行者不存在"),
    FOLLOW_SELF(3004, "不能关注自己"),
    FOLLOW_ALREADY(3005, "已关注该行者"),
    FOLLOW_NOT_FOUND(3006, "关注关系不存在"),
    FORUM_BOARD_NOT_FOUND(3007, "版块不存在"),
    CONVERSATION_NOT_FOUND(3010, "会话不存在"),
    NOT_CONVERSATION_MEMBER(3011, "您不是该会话的参与者"),
    MESSAGE_CONTENT_EMPTY(3012, "消息内容不能为空"),
    MEDIA_TYPE_NOT_SUPPORTED(3013, "暂不支持该媒体类型"),
    MEDIA_FILE_INVALID(3014, "媒体文件无效"),
    STORAGE_CONFIG_INCOMPLETE(3015, "存储配置未完成"),
    FORUM_THREAD_NOT_FOUND(3016, "主题不存在或已删除"),
    FORUM_THREAD_LOCKED(3017, "主题已锁定，暂不可回复"),
    FORUM_REPLY_NOT_FOUND(3018, "引用楼层不存在"),
    FORUM_BOARD_CLOSED(3019, "版块已关闭，暂不可发帖"),
    FORUM_OPERATION_FORBIDDEN(3020, "无权限执行该操作"),
    ACHIEVEMENT_NOT_FOUND(3021, "成就不存在"),
    ACHIEVEMENT_NOT_UNLOCKED(3022, "成就尚未解锁"),
    NOTICE_NOT_FOUND(3023, "通知不存在"),
    CHAT_STRANGER_MESSAGE_LIMIT(3024, "未互相关注时可发送条数已达上限，请等待对方回复");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;
}
