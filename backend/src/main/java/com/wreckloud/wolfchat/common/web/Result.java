package com.wreckloud.wolfchat.common.web;

import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.Data;

/**
 * @Description 统一响应结构
 * @Author Wreckloud
 * @Date 2026-01-06
 * @param <T> 响应数据类型
 */
@Data
public class Result<T> {
    /**
     * 响应码（0表示成功，非0表示失败）
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return build(0, "操作成功", data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return build(0, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> error(Integer code, String message) {
        return build(code, message, null);
    }

    /**
     * 失败响应（使用错误码枚举）
     */
    public static <T> Result<T> error(ErrorCode errorCode) {
        return build(errorCode.getCode(), errorCode.getMessage(), null);
    }

    private static <T> Result<T> build(Integer code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
}


