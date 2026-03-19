package com.wreckloud.wolfchat.common.web;

import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

/**
 * @Description 全局异常处理器
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String LOGIN_PATH = "/auth/login";

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(BaseException.class)
    public Result<?> handleBaseException(BaseException e, HttpServletRequest request) {
        if (isLoginRequest(request)) {
            log.warn("登录业务异常: code={}, uri={}", e.getCode(), resolveRequestUri(request));
            return Result.error(ErrorCode.LOGIN_FAILED);
        }
        log.warn("业务异常: code={}, message={}, uri={}", e.getCode(), e.getMessage(), resolveRequestUri(request));
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数相关异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public Result<?> handleParamException(Exception e, HttpServletRequest request) {
        String message = resolveParamErrorMessage(e);
        if (isLoginRequest(request)) {
            log.info("登录参数异常: uri={}, message={}", resolveRequestUri(request), message);
            return Result.error(ErrorCode.LOGIN_FAILED);
        }
        log.warn("参数异常: uri={}, message={}", resolveRequestUri(request), message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理请求体解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        if (isLoginRequest(request)) {
            log.info("登录请求体异常: uri={}, message={}", resolveRequestUri(request), e.getMessage());
            return Result.error(ErrorCode.LOGIN_FAILED);
        }
        log.warn("请求体异常: uri={}, message={}", resolveRequestUri(request), e.getMessage());
        return Result.error(ErrorCode.PARAM_ERROR);
    }

    /**
     * 处理数据库语法异常
     */
    @ExceptionHandler(BadSqlGrammarException.class)
    public Result<?> handleBadSqlGrammarException(BadSqlGrammarException e, HttpServletRequest request) {
        log.error("数据库结构异常: uri={}", resolveRequestUri(request), e);
        return Result.error(ErrorCode.DATABASE_ERROR);
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        if (isLoginRequest(request)) {
            log.error("登录系统异常: uri={}", resolveRequestUri(request), e);
            return Result.error(ErrorCode.LOGIN_FAILED);
        }
        log.error("系统异常: uri={}", resolveRequestUri(request), e);
        return Result.error(ErrorCode.SYSTEM_ERROR);
    }

    private String resolveParamErrorMessage(Exception e) {
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            if (ex.getBindingResult().hasFieldErrors()) {
                String msg = ex.getBindingResult().getFieldError().getDefaultMessage();
                if (StringUtils.hasText(msg)) {
                    return msg;
                }
            }
        }

        if (e instanceof ConstraintViolationException) {
            ConstraintViolationException ex = (ConstraintViolationException) e;
            if (!ex.getConstraintViolations().isEmpty()) {
                String msg = ex.getConstraintViolations().iterator().next().getMessage();
                if (StringUtils.hasText(msg)) {
                    return msg;
                }
            }
        }

        if (e instanceof IllegalArgumentException && StringUtils.hasText(e.getMessage())) {
            return e.getMessage();
        }

        return ErrorCode.PARAM_ERROR.getMessage();
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        String loginRequestUri = (contextPath == null ? "" : contextPath) + LOGIN_PATH;
        return loginRequestUri.equals(requestUri);
    }

    private String resolveRequestUri(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        return request.getRequestURI();
    }
}
