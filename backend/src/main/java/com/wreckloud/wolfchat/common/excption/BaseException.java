package com.wreckloud.wolfchat.common.excption;

/**
 * @Description 业务异常
 * @Author Wreckloud
 * @Date 2025-12-06
 */
public class BaseException extends RuntimeException {

    private final Integer code;

    public BaseException(ErrorCode errorCode) {
        super(errorCode.msg());
        this.code = errorCode.code();
    }

    public Integer getCode() {
        return code;
    }
}
