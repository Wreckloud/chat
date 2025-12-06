package com.wreckloud.wolfchat.common.excption;

/**
 * @Description 错误码枚举
 * @Author Wreckloud
 * @Date 2025-12-06
 */
public enum ErrorCode {

    PHONE_ALREADY_EXISTS(-1000, "这是一个测试的错误码");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public int code() { return code; }
    public String msg() { return msg; }
}
