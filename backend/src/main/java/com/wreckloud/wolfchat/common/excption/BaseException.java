package com.wreckloud.wolfchat.common.excption;

import lombok.Getter;

/**
 * @Description 自定义异常基类
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Getter
public class BaseException extends RuntimeException {
    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }

    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}


