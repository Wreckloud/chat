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

    SMS_SEND_TOO_FREQUENT(-1014, "发送过于频繁，请稍后再试");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int code() { return code; }
    public String msg() { return msg; }
}
