package com.wreckloud.wolfchat.common.web;

import lombok.Data;

import java.io.Serializable;

/**
 * @Description 后端统一返回结果
 * @Author Wreckloud
 * @Date 2025-12-06
 */
@Data
public class Result<T> implements Serializable {

    private Integer code;   // 编码：1成功，0和其它数字为失败
    private String msg;     // 错误信息
    private T data;         // 实际数据

    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(0, "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "success", data);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(-1, msg, null);
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}
