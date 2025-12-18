package com.wreckloud.wolfchat.common.web;

import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

/**
 * @Description 全局异常处理器
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBaseException(BaseException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.warn("参数校验异常: {}", message);
        return Result.fail(-1000, message);
    }

    /**
     * 处理参数校验异常（@ModelAttribute）
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.warn("参数校验异常: {}", message);
        return Result.fail(-1000, message);
    }

    /**
     * 处理参数校验异常（@RequestParam）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = violations.iterator().hasNext() 
                ? violations.iterator().next().getMessage() 
                : "参数校验失败";
        log.warn("参数校验异常: {}", message);
        return Result.fail(-1000, message);
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        
        // 开发环境返回详细错误信息，生产环境返回通用提示
        // TODO: 根据配置判断环境
        String message = "系统繁忙，请稍后重试";
        
        // 对于常见的空指针等异常，给出更友好的提示
        if (e instanceof NullPointerException) {
            message = "数据处理异常，请检查请求参数";
            log.error("空指针异常详情: ", e);
        } else if (e instanceof IllegalArgumentException) {
            message = "参数异常: " + e.getMessage();
        }
        
        return Result.fail(-9999, message);
    }
}

