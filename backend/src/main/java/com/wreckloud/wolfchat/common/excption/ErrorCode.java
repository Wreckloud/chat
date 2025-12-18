package com.wreckloud.wolfchat.common.excption;

/**
 * @Description 错误码枚举
 * @Author Wreckloud
 * @Date 2025-12-06
 */
public enum ErrorCode {


    MOBILE_ALREADY_EXISTS(-1001, "手机号已被注册"),
    CAPTCHA_ERROR(-1002, "验证码错误或已过期"),
    CAPTCHA_EMPTY(-1003, "验证码不能为空"),

    NO_AVAILABLE_NUMBER(-1004, "暂无可用号码，请稍后再试"),

    PASSWORD_MISMATCH(-1005, "两次输入的密码不一致"),

    INVALID_PARAM(-1006, "参数无效"),

    INVALID_NUMBER(-1007, "号码格式无效，必须是6-10位数（100000-9999999999）"),

    NUMBER_ALREADY_EXISTS(-1008, "号码已存在"),

    NUMBER_GENERATION_FAILED(-1009, "号码生成失败，可能号码池接近饱和"),

    NUMBER_NOT_FOUND(-1010, "号码不存在"),

    NUMBER_IN_USE(-1011, "号码已被使用，无法删除"),

    USER_DISABLED(-1012, "用户已被禁用或注销"),

    SMS_CODE_ERROR(-1013, "短信验证码错误或已过期"),

    SMS_SEND_TOO_FREQUENT(-1014, "发送过于频繁，请稍后再试"),

    // 认证相关错误码 (1000-1999)
    UNAUTHORIZED(-1001, "未登录或登录已过期"),
    TOKEN_INVALID(-1002, "Token无效"),
    TOKEN_EXPIRED(-1003, "Token已过期"),
    NO_PERMISSION(-1004, "无权限访问"),

    // 群组相关错误码 (2000-2999)
    GROUP_NOT_FOUND(-2001, "群组不存在"),
    GROUP_MEMBER_FULL(-2002, "群成员已满"),
    GROUP_ALREADY_JOINED(-2003, "已经在群内"),
    NO_GROUP_PERMISSION(-2004, "无权限操作"),
    GROUP_OWNER_CANNOT_QUIT(-2005, "群主不能退出群组，请先转让群主或解散群组"),
    GROUP_DISBANDED(-2006, "群组已解散"),
    GROUP_MEMBER_NOT_FOUND(-2007, "该用户不在群内"),
    CANNOT_KICK_OWNER(-2008, "不能踢出群主"),
    CANNOT_KICK_ADMIN(-2009, "管理员不能踢出其他管理员"),
    USER_NOT_FOUND(-2010, "用户不存在");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int code() { return code; }
    public String msg() { return msg; }
}
